package com.qandil.airesumeanalyzer

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object ResumeReaders {

    private fun guessMime(context: Context, uri: Uri): String {
        return context.contentResolver.getType(uri)
            ?: MimeTypeMap.getFileExtensionFromUrl(uri.toString())?.let { ext ->
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase())
            } ?: "application/octet-stream"
    }

    /** Returns extracted text or empty string on failure. */
    suspend fun readResumeText(context: Context, uri: Uri): String {
        val mime = guessMime(context, uri)
        return runCatching {
            when {
                mime.startsWith("text/") || uri.toString().endsWith(".txt", true) -> {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                }
                mime == "application/pdf" || uri.toString().endsWith(".pdf", true) -> {
                    PDFBoxResourceLoader.init(context.applicationContext)
                    context.contentResolver.openInputStream(uri)?.use { ins ->
                        PDDocument.load(ins).use { doc ->
                            PDFTextStripper().getText(doc)
                        }
                    } ?: ""
                }
                mime == "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        || uri.toString().endsWith(".docx", true) -> {
                    context.contentResolver.openInputStream(uri)?.use { ins ->
                        extractDocxText(ins)
                    } ?: ""
                }
                else -> ""
            }
        }.getOrElse { "" }
    }

    // --- Minimal DOCX text extractor: reads word/document.xml from the ZIP and strips tags ---
    private fun extractDocxText(ins: InputStream): String {
        ZipInputStream(ins).use { zis ->
            var entry: ZipEntry?
            while (true) {
                entry = zis.nextEntry ?: break
                if (entry!!.name == "word/document.xml") {
                    val xml = zis.bufferedReaderUtf8().use { it.readText() }
                    return xmlToPlainText(xml)
                }
            }
        }
        return ""
    }

    private fun ZipInputStream.bufferedReaderUtf8(): BufferedReader =
        BufferedReader(InputStreamReader(this, Charsets.UTF_8))

    private fun xmlToPlainText(xml: String): String {
        // Add newlines at paragraph boundaries to keep rough structure
        var s = xml
            .replace("</w:p>", "\n")
            .replace("<w:tab[^>]*/>".toRegex(), "\t")

        // Remove all tags
        s = s.replace("<[^>]+>".toRegex(), " ")

        // Decode the most common XML entities
        s = s.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")

        // Collapse whitespace
        s = s.lines().joinToString("\n") { it.trim() }
        s = s.replace("\\s+".toRegex(), " ").replace(" \n", "\n").trim()
        return s
    }
}
