// shared/core/JDMatch.kt
package com.qandil.airesume.shared.core

import com.qandil.airesume.shared.model.JDMatchResult
import kotlin.math.roundToInt

object JDMatch {

    // ---------- domain dictionaries (lowercase) ----------
    private val STOP_WORDS = setOf(
        "a","an","the","and","or","but","of","in","on","to","for","with","by","at","as",
        "is","are","was","were","be","been","being","that","this","those","these","from",
        "it","its","into","about","across","per","via","than","then","over","under","out",
        "your","you","we","our","their","they","he","she","i","me","my","mine","him","her",
        "will","shall","can","could","should","would","may","might","must","not","no","yes",
        "etc","eg","e.g","ie","i.e","including","include","includes","such","like"
    )

    private val MONTHS = setOf(
        "january","february","march","april","may","june","july","august","september",
        "october","november","december","jan","feb","mar","apr","jun","jul","aug",
        "sep","sept","oct","nov","dec"
    )

    // generic JD words / calendar junk to ignore
    private val GENERIC_JOB_WORDS = setOf(
        "job","role","position","vacancy","opening","deadline","date","monday","tuesday",
        "wednesday","thursday","friday","saturday","sunday","am","pm","hour","hours",
        "week","weeks","month","months","year","years","today","tomorrow","start",
        "starting","end","ending","day","days"
    )

    // noise patterns
    private val PHONE_REGEX   = Regex("""\+?\d[\d\s\-()]{6,}""")
    private val EMAIL_REGEX   = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")
    private val URL_REGEX     = Regex("""https?://\S+|www\.\S+""")
    private val ORDINAL_REGEX = Regex("""\b\d+(st|nd|rd|th)\b""")
    private val NUMBER_REGEX  = Regex("""\b\d+(?:[.,]\d+)?\b""")

    // multi-word phrases (match first, keep as single tokens)
    private val PHRASE_SKILLS = listOf(
        "kotlin multiplatform","jetpack compose","android jetpack","rest api","unit testing",
        "ui testing","test driven development","clean architecture","dependency injection",
        "continuous integration","continuous delivery","google play","material design",
        "room database","sql database","firebase auth","firebase firestore",
        "firebase cloud messaging","android studio","dagger hilt","view model","live data",
        "coroutines flow","background work","work manager","performance profiling",
        "crash analytics","compose navigation","data binding","retrofit client",
        "okhttp client","graphql api","kmm","kmp"
    ).sortedByDescending { it.length }

    // single-token tech skills
    private val TOKEN_SKILLS = setOf(
        "kotlin","android","compose","coroutines","flow","room","retrofit","okhttp",
        "dagger","hilt","mvvm","mvi","kmp","kmm","sqlite","graphql","firebase","auth",
        "firestore","crashlytics","analytics","gradle","git","jenkins","jira","sql",
        "ci","cd","json","xml","oauth","jwt","websocket","bluetooth","beacon","maps",
        "koin","ktor","coil","glide","picasso","espresso","junit","mockk","composeui",
        "material3"
    )

    // responsibilities / soft skills we want to mirror
    private val RESPONSIBILITY_TERMS = setOf(
        "design","architect","build","deliver","ship","optimize","profile","refactor",
        "review","mentor","lead","coach","collaborate","coordinate","own","drive","scale",
        "migrate","integrate","automate","monitor","debug","test","document","plan",
        "estimate","present","communicate","stakeholder","ownership"
    )

    private val SOFT_SKILLS = setOf(
        "leadership","communication","collaboration","mentorship","initiative",
        "problem solving","teamwork","stakeholder management","time management",
        "ownership","accountability","adaptability"
    )

    // ---------- public API ----------
    fun matchResumeToJD(resumeText: String, jdText: String): JDMatchResult {
        val r = tokenize(resumeText)
        val j = tokenize(jdText)

        val resumeTerms = r.tokens.keys + r.phrases.keys
        val jdTerms     = j.tokens.keys + j.phrases.keys

        // light stemming to align simple variants
        val resumeCanon = resumeTerms.map(::stem).toSet()
        val jdCanon     = jdTerms.map(::stem).toSet()

        val present = jdCanon.filter { it in resumeCanon }.toSet()
        val missing = jdCanon.filter { it !in resumeCanon }.toSet()

        // weights from JD side (importance)
        val weights = weightMap(j.tokens, j.phrases)

        // categorize missing
        val missingSkills = missing.filter { it in TOKEN_SKILLS || PHRASE_SKILLS.contains(it) }
        val missingResp   = missing.filter { it in RESPONSIBILITY_TERMS }
        val missingSoft   = missing.filter { it in SOFT_SKILLS }

        fun <T> topN(c: Collection<T>, n: Int) =
            c.sortedByDescending { weights[it.toString()] ?: 0.0 }.take(n)

        val topMissingSkills = topN(missingSkills, 12)
        val topMissingResp   = topN(missingResp, 8)
        val topMissingSoft   = topN(missingSoft, 6)

        // weighted match score
        val presentScore = present.sumOf { weights[it] ?: 0.0 }
        val totalScore   = jdCanon.sumOf { weights[it] ?: 1.0 }.let { if (it <= 0.0) 1.0 else it }
        val matchScore   = ((presentScore / totalScore) * 100.0).roundToInt().coerceIn(0, 100)

        // actionable suggestions
        val suggestions = buildList {
            if (topMissingSkills.isNotEmpty())
                add("Add to 'Skills' section: ${topMissingSkills.joinToString()}.")
            if (topMissingResp.isNotEmpty())
                add("Mirror responsibilities in bullets: ${topMissingResp.joinToString()}.")
            if (topMissingSoft.isNotEmpty())
                add("Weave soft skills where true: ${topMissingSoft.joinToString()}.")
            if (isEmpty())
                add("Great match. Mirror 2–3 JD terms in recent bullets where truthful.")
        }

        return JDMatchResult(
            matchScore = matchScore,
            missingKeywords = (topMissingSkills + topMissingResp + topMissingSoft),
            tailoredSuggestions = suggestions,
            resumeKeywords = present.sorted(),
            // grouped outputs for richer UI
            missingSkills = topMissingSkills,
            missingResponsibilities = topMissingResp,
            missingSoftSkills = topMissingSoft
        )
    }

    // ---------- helpers ----------
    private data class Extracted(val tokens: Map<String, Int>, val phrases: Map<String, Int>)

    private fun clean(text: String): String =
        text.lowercase()
            .replace(EMAIL_REGEX, " ")
            .replace(URL_REGEX, " ")
            .replace(PHONE_REGEX, " ")

    private fun tokenize(text: String): Extracted {
        val cleaned = clean(text)
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        // detect phrases first and remove them to avoid double count
        val phraseCounts = mutableMapOf<String, Int>()
        var tmp = cleaned
        for (p in PHRASE_SKILLS) {
            val rx = Regex("""\b${Regex.escape(p)}\b""")
            val count = rx.findAll(tmp).count()
            if (count > 0) {
                phraseCounts[p] = count
                tmp = tmp.replace(rx, " ")
            }
        }

        val raw = tmp.split(" ").filter { it.isNotBlank() }
        val tokens = mutableMapOf<String, Int>()
        for (t in raw) {
            if (t.length < 3) continue
            if (t in STOP_WORDS || t in MONTHS || t in GENERIC_JOB_WORDS) continue
            if (ORDINAL_REGEX.matches(t) || NUMBER_REGEX.matches(t)) continue
            tokens[t] = (tokens[t] ?: 0) + 1
        }
        return Extracted(tokens, phraseCounts)
    }

    private fun stem(s: String): String =
        s.removeSuffix("ing").removeSuffix("ed").removeSuffix("s")

    private fun weightMap(tokens: Map<String, Int>, phrases: Map<String, Int>): Map<String, Double> {
        val scores = mutableMapOf<String, Double>()
        for ((k, c) in phrases) scores[k] = 3.0 * c
        for ((k, c) in tokens) {
            val base = when {
                k in TOKEN_SKILLS -> 2.0
                k in RESPONSIBILITY_TERMS -> 1.5
                k in SOFT_SKILLS -> 1.2
                else -> 1.0
            }
            scores[k] = (scores[k] ?: 0.0) + base * c
        }
        return scores
    }
}
