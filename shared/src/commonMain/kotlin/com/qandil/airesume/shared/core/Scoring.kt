package com.qandil.airesume.shared.core

import com.qandil.airesume.shared.model.*
import kotlin.math.roundToInt

object Scoring {

    // ===== General dictionaries (resume-only) =====
    private val ACTION_VERBS = setOf(
        "led","built","designed","implemented","optimized","delivered","launched",
        "migrated","refactored","architected","reduced","improved","increased",
        "automated","developed","deployed","mentored","owned","integrated","scaled"
    )

    // ===== Resume-only analysis (no domain skill lists) =====
    fun analyzeResumeText(text: String): AnalysisResult {
        val cleaned = normalize(text)

        val words = cleaned.split(Regex("\\s+")).filter { it.isNotBlank() }
        val wordCount = words.size

        val actionVerbRateRatio = calcActionVerbRate(cleaned)

        // Count bullets that include a number, %, or currency symbol
        val quantifiedBullets = Regex(
            pattern = "(^|\\n)\\s*[-•]\\s*.*?(%|\\b\\d+\\b|£|\\$)",
            options = setOf(RegexOption.IGNORE_CASE)
        ).findAll(cleaned).count()

        val hasGithub = Regex("github\\.com", setOf(RegexOption.IGNORE_CASE)).containsMatchIn(cleaned)
        val hasLinkedIn = Regex("linkedin\\.com", setOf(RegexOption.IGNORE_CASE)).containsMatchIn(cleaned)
        val hasPortfolio = Regex("(portfolio|devfolio|website)", setOf(RegexOption.IGNORE_CASE)).containsMatchIn(cleaned)

        // ----- Scores (domain-neutral) -----
        val contentScore = clamp(
            sectionPresent(cleaned, "summary").score(3) +
                    sectionPresent(cleaned, "experience").score(8) +
                    sectionPresent(cleaned, "education").score(4) +
                    sectionPresent(cleaned, "skills").score(3),
            0, 20
        )

        val impactScore = clamp(
            (actionVerbRateRatio * 20).roundToInt().coerceAtMost(8) +       // action verbs
                    quantifiedBullets.coerceAtMost(8),                               // metrics
            0, 20
        )

        // “Relevance” redefined to be neutral: shows signals that tend to correlate
        // with employability across roles (projects + links + compact length)
        val relevanceScore = clamp(
            (if (Regex("^\\s*projects", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)).containsMatchIn(cleaned)) 6 else 0) +
                    (if (hasGithub || hasPortfolio) 6 else 0) +
                    (if (wordCount in 450..900) 4 else 0) +
                    (if (actionVerbRateRatio >= 0.5) 4 else 0),
            0, 20
        )

        val recencyScore = 12  // placeholder until you parse dates
        val formatScore = (
                (if (wordCount in 450..900) 2 else 0) +
                        (if (!Regex("\\t|\\|\\|", setOf(RegexOption.IGNORE_CASE)).containsMatchIn(cleaned)) 6 else 0) +
                        (if (Regex("^\\s*(experience|education|skills|projects)", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)).containsMatchIn(cleaned)) 4 else 0) +
                        (if (hasGithub || hasLinkedIn) 3 else 0)
                ).coerceAtMost(15)

        val languageScore = 9 // simple placeholder

        val subs = Subscores(contentScore, impactScore, relevanceScore, recencyScore, formatScore, languageScore)
        val overall = contentScore + impactScore + relevanceScore + recencyScore + formatScore + languageScore

        val recs = buildRecommendations(
            wordCount = wordCount,
            actionVerbRate = actionVerbRateRatio,
            quantifiedBullets = quantifiedBullets,
            hasGithub = hasGithub,
            hasLinkedIn = hasLinkedIn
        )

        // IMPORTANT: in resume-only mode we DO NOT emit matched/missing skills
        return AnalysisResult(
            overall = overall,
            subscores = subs,
            signals = Signals(
                wordCount = wordCount,
                actionVerbRate = actionVerbRateRatio,
                quantifiedBullets = quantifiedBullets,
                lastRoleMonthsAgo = null,
                skillsMatched = emptyList(),      // ← empty in resume-only mode
                skillsMissing = emptyList(),      // ← empty in resume-only mode
                links = Links(hasGithub, hasLinkedIn, hasPortfolio)
            ),
            recommendations = recs
        )
    }

    // ----- Helpers -----
    private fun normalize(t: String) = t.replace("\r", "").trim()

    private fun sectionPresent(text: String, name: String): String {
        val rx = Regex("^\\s*(?:$name|professional summary|profile)\\b",
            setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))
        return if (rx.containsMatchIn(text)) name else ""
    }

    private fun calcActionVerbRate(text: String): Double {
        val lines = text.lines()
            .map { it.trim() }
            .filter { it.startsWith("-") || it.startsWith("•") }
        if (lines.isEmpty()) return 0.0
        val withVerb = lines.count { line ->
            val first = line.removePrefix("-").removePrefix("•").trim()
                .split(" ").firstOrNull()?.lowercase()
            first != null && ACTION_VERBS.contains(first)
        }
        return withVerb.toDouble() / lines.size
    }

    private fun buildRecommendations(
        wordCount: Int,
        actionVerbRate: Double,
        quantifiedBullets: Int,
        hasGithub: Boolean,
        hasLinkedIn: Boolean
    ): List<Recommendation> {
        val recs = mutableListOf<Recommendation>()
        var p = 1

        if (wordCount !in 450..900) {
            val delta = if (wordCount > 900)
                "Reduce length from ~$wordCount to 450–900 words (1–2 pages)"
            else
                "Add content up to ~450 words"
            recs += Recommendation(p++, "$delta. Keep last 10 years; group older roles as 'Earlier Experience'.")
        }

        if (actionVerbRate < 0.5) {
            recs += Recommendation(
                p++,
                "Rewrite bullets to start with action verbs (target ≥50%). E.g., 'Optimized cold start by 22% via lazy Compose rendering.'"
            )
        }

        if (quantifiedBullets < 5) {
            recs += Recommendation(
                p++,
                "Add metrics to ≥5 bullets (%, time, crash rate, MAU). E.g., 'Reduced ANR from 0.9%→0.2% by moving I/O off main thread.'"
            )
        }

        if (!hasGithub)   recs += Recommendation(p++, "Add your GitHub link to showcase code or notebooks.")
        if (!hasLinkedIn) recs += Recommendation(p++, "Add your LinkedIn profile link in the header.")

        if (wordCount > 1200) {
            recs += Recommendation(
                p++,
                "Restructure: Summary (3–4 lines) • Skills (single line) • Experience (5–6 bullets total) • Education • Links."
            )
        }

        if (recs.isEmpty()) recs += Recommendation(p, "Strong structure — consider a brief impact-focused Summary at the top.")
        return recs
    }

    private fun String.score(max: Int) = if (this.isNotBlank()) max else 0
    private fun clamp(v: Int, min: Int, max: Int) = v.coerceIn(min, max)

    // ===== JD match stays responsible for skills/keywords =====
    // Keep your improved JD matching code (weighted keywords, phrases, stopwords)
    // exactly as you already implemented it. Resume-only mode will not call it.
}
