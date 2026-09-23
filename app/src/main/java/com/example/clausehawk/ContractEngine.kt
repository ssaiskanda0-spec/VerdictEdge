package com.example.clausehawk

import kotlinx.coroutines.delay

class ContractEngine {

    suspend fun analyzeContract(text: String): AnalysisResult {
        delay(2000L) // Simulate Gemma inference timing

        val risk = when {
            text.contains("indemnify", ignoreCase = true) || text.contains("without limitation", ignoreCase = true) -> RiskLevel.HIGH
            text.contains("terminate", ignoreCase = true) || text.contains("consequential", ignoreCase = true) -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        val redFlags = when (risk) {
            RiskLevel.HIGH -> listOf(
                RedFlag("Uncapped Liability", "Indemnification section lacks a financial liability ceiling."),
                RedFlag("Broad Indemnity", "Broad obligation covers indirect damages and third-party claims."),
                RedFlag("Unilateral Obligation", "Obligation imposed exclusively on the receiving party.")
            )
            RiskLevel.MEDIUM -> listOf(
                RedFlag("Termination Notice", "Standard 30-day notice period required for termination."),
                RedFlag("Consequential Exclusion", "Consequential damages are excluded for both parties."),
                RedFlag("Jurisdiction Scope", "Verify dispute resolution jurisdiction in local courts.")
            )
            RiskLevel.LOW -> listOf(
                RedFlag("Standard Mutual Terms", "Standard mutual termination conditions applied."),
                RedFlag("Balanced Liability", "No unusual liability shifts or hidden fee clauses found."),
                RedFlag("Clear Ownership", "Balanced confidentiality & ownership scope.")
            )
        }

        val summaryText = if (text.length > 100) text.take(160) + "..." else text

        return AnalysisResult(
            riskLevel = risk,
            summary = summaryText,
            redFlags = redFlags
        )
    }
}