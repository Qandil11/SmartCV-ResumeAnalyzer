package com.qandil.airesume.shared.model


data class AnalysisResult(
    val version: String = "1.0",
    val jobProfile: String = "Android | Data | AI",
    val overall: Int,
    val subscores: Subscores,
    val signals: Signals,
    val recommendations: List<Recommendation>
)


data class Subscores(
    val content: Int,
    val impact: Int,
    val relevance: Int,
    val recency: Int,
    val format: Int,
    val language: Int
)
data class JDMatchResult(
    val matchScore: Int,
    val missingKeywords: List<String>,
    val tailoredSuggestions: List<String>,
    val resumeKeywords: List<String>,
    // New grouped outputs
    val missingSkills: List<String> = emptyList(),
    val missingResponsibilities: List<String> = emptyList(),
    val missingSoftSkills: List<String> = emptyList()
)



data class Signals(
    val wordCount: Int,
    val actionVerbRate: Double,
    val quantifiedBullets: Int,
    val lastRoleMonthsAgo: Int?,
    val skillsMatched: List<String>,
    val skillsMissing: List<String>,
    val links: Links
)


data class Links(val github: Boolean, val linkedin: Boolean, val portfolio: Boolean)


data class Recommendation(val priority: Int, val text: String)