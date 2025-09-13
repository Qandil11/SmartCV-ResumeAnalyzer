package com.qandil.airesumeanalyzer

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qandil.airesume.shared.core.JDMatch
import com.qandil.airesume.shared.core.Scoring
import com.qandil.airesume.shared.model.AnalysisResult
import com.qandil.airesume.shared.model.JDMatchResult

private enum class Screen { Resume, JD }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppUI() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUI() {
    val context = LocalContext.current
    val SHOW_DEBUG = false
    ExitOnBack()
    // navigation
    var screen by rememberSaveable { mutableStateOf(Screen.Resume) }

    // resume state
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var resumeText by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf<AnalysisResult?>(null) }

    // JD state
    var jdText by rememberSaveable { mutableStateOf("") }
    var jdMatch by remember { mutableStateOf<JDMatchResult?>(null) }

    // dialogs
    var showPasteResume by remember { mutableStateOf(false) }
    var pasteBuffer by rememberSaveable { mutableStateOf("") }
    var showPasteJD by remember { mutableStateOf(false) }

    // file IO state
    var isLoading by remember { mutableStateOf(false) }
    var lastMime by remember { mutableStateOf<String?>(null) }
    var lastBytes by remember { mutableStateOf<Long?>(null) }
    var lastError by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pickedUri = uri
    }
    LaunchedEffect(resumeText) {
        result = if (resumeText.isNotBlank()) Scoring.analyzeResumeText(resumeText) else null
    }


    LaunchedEffect(resumeText, jdText) {
        val ok = resumeText.isNotBlank() && jdText.trim().length >= 40
        jdMatch = if (ok) JDMatch.matchResumeToJD(resumeText, jdText) else null
    }

    LaunchedEffect(pickedUri) {
        pickedUri?.let { uri ->
            isLoading = true
            lastError = null
            val text = ResumeReaders.readResumeText(context, uri)
            resumeText = text
            isLoading = false
            if (resumeText.isBlank()) lastError = "Couldn’t extract text. Try Paste Resume."
            // result + jdMatch will be updated by the effects watching resumeText/jdText
        }
    }


    Scaffold(topBar = { TopAppBar(title = { Text("AI Resume Analyzer") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Tabs
            TabRow(selectedTabIndex = screen.ordinal) {
                Tab(
                    selected = screen == Screen.Resume,
                    onClick = { screen = Screen.Resume },
                    text = { Text("Resume") }
                )
                Tab(
                    selected = screen == Screen.JD,
                    onClick = { screen = Screen.JD },
                    text = { Text("JD Match") }
                )
            }

            // Contextual actions
            when (screen) {
                Screen.Resume -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            picker.launch(
                                arrayOf(
                                    "text/plain",
                                    "application/pdf",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                )
                            )
                        }) { Text("Import Resume") }

                        Button(onClick = { showPasteResume = true }) { Text("Paste Resume") }


                    }

                    if (SHOW_DEBUG) {
                        DebugStrip(
                            isLoading = isLoading,
                            mime = lastMime,
                            size = lastBytes,
                            error = lastError,
                            hasText = resumeText.isNotBlank()
                        )
                    }

                    when {
                        isLoading -> CenterLoader()
                        result == null -> AssistiveText("Import or paste your resume to analyze.")
                        else -> ResultScreen(r = result!!)
                    }
                }

                Screen.JD -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { showPasteJD = true }) { Text("Paste JD") }


                    }

                    if (resumeText.isBlank()) {
                        AssistiveText("First load your resume on the Resume tab.")
                    }

                    if (jdMatch != null) {
                        ResultJDSection(jdMatch!!)
                    } else if (jdText.isBlank()) {
                        AssistiveText("Paste a job description, then tap Done.")
                    }
                }
            }
        }
    }

    /* Dialogs */
    if (showPasteResume) {
        AlertDialog(
            onDismissRequest = { showPasteResume = false },
            title = { Text("Paste Resume (text)") },
            text = {
                OutlinedTextField(
                    value = pasteBuffer,
                    onValueChange = { pasteBuffer = it },
                    minLines = 10,
                    maxLines = 20,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Paste the resume text here…") }
                )
            },
            // inside if (showPasteResume) { AlertDialog(...) }
            confirmButton = {
                TextButton(onClick = {
                    resumeText = pasteBuffer
                    pasteBuffer = ""
                    showPasteResume = false
                    // jdMatch will auto-update via the LaunchedEffect above
                }) { Text("Done") }
            }
            ,
            dismissButton = {
                TextButton(onClick = { showPasteResume = false }) { Text("Cancel") }
            }
        )
    }

    if (showPasteJD) {
        AlertDialog(
            onDismissRequest = { showPasteJD = false },
            title = { Text("Paste Job Description") },
            text = {
                OutlinedTextField(
                    value = jdText,
                    onValueChange = { jdText = it },
                    minLines = 10,
                    maxLines = 20,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Paste the JD here…") }
                )
            },
            // inside if (showPasteJD) { AlertDialog(...) }
            confirmButton = {
                TextButton(onClick = {
                    showPasteJD = false
                    // optional: force now; otherwise LaunchedEffect will handle it
                    val ok = resumeText.isNotBlank() && jdText.trim().length >= 40
                    jdMatch = if (ok) JDMatch.matchResumeToJD(resumeText, jdText) else null
                }) { Text("Done") }
            }

            ,
            dismissButton = {
                TextButton(onClick = { showPasteJD = false }) { Text("Cancel") }
            }
        )
    }
}

/* ---------- Resume analysis UI ---------- */

@Composable
fun ResultScreen(r: AnalysisResult) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item { OverallGauge(r.overall) }

        item {
            SectionCard("What we found") {
                DiagnosticRow(
                    label = "Length",
                    value = "${r.signals.wordCount}",
                    bad = r.signals.wordCount !in 450..900,
                    tip = "Aim for 450–900 words (1–2 pages)."
                )
                DiagnosticRow(
                    label = "Action verbs",
                    value = "${(r.signals.actionVerbRate * 100).toInt()}%",
                    bad = r.signals.actionVerbRate < 0.5,
                    tip = "≥50% of bullets should start with a verb."
                )
                DiagnosticRow(
                    label = "Quantified bullets",
                    value = "${r.signals.quantifiedBullets}",
                    bad = r.signals.quantifiedBullets < 5,
                    tip = "Add ≥5 bullets with %, Δ, time, $, MAU."
                )
            }
        }

        item {
            SectionCard("Subscores") {
                ScoreBar("Content", r.subscores.content)
                ScoreBar("Impact", r.subscores.impact)
                ScoreBar("Relevance", r.subscores.relevance)
                ScoreBar("Recency", r.subscores.recency)
                ScoreBar("Format", r.subscores.format)
                ScoreBar("Language", r.subscores.language)
            }
        }

        item {
            SectionCard("Skills focus") {
                Text("Matched", style = MaterialTheme.typography.labelLarge)
                FlowRow { if (r.signals.skillsMatched.isEmpty()) Text("—") else r.signals.skillsMatched.forEach { Chip(it) } }
                Spacer(Modifier.height(6.dp))
                Text("Add these", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
                FlowRow { if (r.signals.skillsMissing.isEmpty()) Text("—") else r.signals.skillsMissing.forEach { Chip(it) } }
            }
        }

        item {
            SectionCard("How to fix (next 5 edits)") {
                r.recommendations.take(5).forEach { rec ->
                    Text("• ${rec.text}", maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/* ---------- JD match UI ---------- */

@Composable
fun ResultJDSection(m: JDMatchResult) {
    SectionCard("JD Match") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Match score")
            val color = when {
                m.matchScore >= 80 -> MaterialTheme.colorScheme.primary
                m.matchScore >= 60 -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.error
            }
            Text("${m.matchScore} / 100", color = color)
        }
        Spacer(Modifier.height(8.dp))



        Chips("Missing skills", m.missingSkills, error = true)
        Chips("Missing responsibilities", m.missingResponsibilities, error = true)
        Chips("Missing soft skills", m.missingSoftSkills, error = true)

        Text("Tailored suggestions", style = MaterialTheme.typography.labelLarge)
        m.tailoredSuggestions.forEach { Text("• $it", maxLines = 3, overflow = TextOverflow.Ellipsis) }

        if (m.resumeKeywords.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Present in resume", style = MaterialTheme.typography.labelLarge)
            FlowRow { m.resumeKeywords.take(16).forEach { Chip(it) } }
        }
    }
}
@Composable
fun Chips(title: String, items: List<String>, error: Boolean = false) {
    if (items.isEmpty()) return
    Text(title, style = MaterialTheme.typography.labelLarge,
        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
    FlowRow { items.forEach { Chip(it) } }
    Spacer(Modifier.height(6.dp))
}
/* ---------- building blocks ---------- */

@Composable
private fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun OverallGauge(overall: Int) {
    SectionCard(title = "Overall") {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
            CircularProgressIndicator(
                progress = (overall.coerceIn(0, 100)) / 100f,
                strokeWidth = 8.dp,
                modifier = Modifier.fillMaxSize()
            )
            Text("$overall", style = MaterialTheme.typography.headlineMedium)
        }
        Text(
            when {
                overall >= 85 -> "Excellent"
                overall >= 70 -> "Good"
                overall >= 55 -> "Fair"
                else -> "Needs improvement"
            },
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ScoreBar(label: String, value: Int, max: Int = 20) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("$value / $max", style = MaterialTheme.typography.labelSmall)
        }
        LinearProgressIndicator(
            progress = (value.coerceIn(0, max)) / max.toFloat(),
            modifier = Modifier.fillMaxWidth().height(8.dp)
        )
    }
}

@Composable
private fun Chip(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.padding(end = 6.dp, bottom = 6.dp)
    ) { Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) }
}

@Composable
private fun DiagnosticRow(label: String, value: String, bad: Boolean, tip: String) {
    val color = if (bad) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text(value, color = color)
        }
        if (bad) Text(tip, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun FlowRow(content: @Composable () -> Unit) {
    Column { Row(Modifier.fillMaxWidth()) { content() } }
}

@Composable
private fun AssistiveText(msg: String) {
    Text(msg, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun CenterLoader() {
    Row(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.Center) {
        CircularProgressIndicator()
    }
}

/** Debug-only status line */
@Composable
private fun DebugStrip(isLoading: Boolean, mime: String?, size: Long?, error: String?, hasText: Boolean) {
    val status = when {
        isLoading -> "Loading…"
        error != null -> "Error: $error"
        hasText -> "Loaded OK"
        else -> "No text yet"
    }
    val detail = "mime=${mime ?: "-"}, size=${size ?: "-"}"
    Text("Status: $status  |  $detail", style = MaterialTheme.typography.labelSmall)
}
@Composable
fun ExitOnBack() {
    val context = LocalContext.current
    BackHandler {
        (context as? ComponentActivity)?.finish()
    }
}