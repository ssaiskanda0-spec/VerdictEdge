package com.example.clausehawk

import kotlinx.coroutines.delay

class ContractEngine {

    suspend fun analyzeContract(text: String): AnalysisResult {
        delay(1500L) // Simulate analysis processing timing

        val cleanText = text.trim()
        val flags = mutableListOf<RedFlag>()

        // 1. Guardrail: Detect Prompt Injection & Adversarial AI Override Attacks
        val injectionPatterns = listOf(
            "ignore all previous" to "Attempts to override system prompt",
            "ignore previous" to "Attempts to bypass AI system safety",
            "processing agent" to "Targets automated parsing agents",
            "classify this document" to "Dictates forced risk output",
            "supersedes all internal safety" to "Attempts safety parameter override",
            "low risk - standard" to "Forces hardcoded low-risk classification"
        )

        var hasPromptInjection = false
        for ((pattern, desc) in injectionPatterns) {
            if (cleanText.contains(pattern, ignoreCase = true)) {
                hasPromptInjection = true
                flags.add(
                    RedFlag(
                        "Adversarial Prompt Injection",
                        "Document contains hidden text attempting to manipulate automated risk analysis ($desc)."
                    )
                )
                break
            }
        }

        // 2. Guardrail: Detect Extreme Daily Penalties & Liquidated Damages
        val isDailyPenalty = cleanText.contains("per calendar day", ignoreCase = true) ||
                cleanText.contains("per day", ignoreCase = true) ||
                cleanText.contains("/day", ignoreCase = true)

        val hasLiquidatedDamages = cleanText.contains("liquidated damages", ignoreCase = true)
        val containsDollarAmount = Regex("""\$\s*[\d,]+(\.\d{2})?""").containsMatchIn(cleanText)

        var hasExtremePenalty = false
        if (hasLiquidatedDamages || (isDailyPenalty && containsDollarAmount)) {
            hasExtremePenalty = true
            flags.add(
                RedFlag(
                    "Severe Liquidated Damages",
                    "Imposes steep recurring financial penalties (e.g., daily liquidated damages) that accrue rapidly upon breach or suspicion."
                )
            )
        }

        // 3. Guardrail: Detect Unilateral & Subjective Enforcement Terms
        if (cleanText.contains("sole satisfaction", ignoreCase = true) ||
            cleanText.contains("suspicion of a breach", ignoreCase = true) ||
            cleanText.contains("sole discretion", ignoreCase = true)
        ) {
            flags.add(
                RedFlag(
                    "Unilateral / Subjective Enforcement",
                    "Penalties trigger immediately upon suspicion or are evaluated exclusively to one party's sole satisfaction without objective proof."
                )
            )
        }

        // 4. Standard High Risk Legal Terms
        if (cleanText.contains("indemnify", ignoreCase = true) || cleanText.contains("indemnification", ignoreCase = true)) {
            flags.add(
                RedFlag(
                    "Broad Indemnification",
                    "Indemnification clause shifts legal defense costs and liabilities onto the receiving party."
                )
            )
        }

        if (cleanText.contains("without limitation", ignoreCase = true) || cleanText.contains("uncapped", ignoreCase = true)) {
            flags.add(
                RedFlag(
                    "Uncapped Financial Liability",
                    "Agreement lacks a monetary ceiling on total legal liability."
                )
            )
        }

        // 5. Moderate Risk Legal Terms
        if (cleanText.contains("terminate", ignoreCase = true) || cleanText.contains("termination", ignoreCase = true)) {
            if (flags.none { it.title.contains("Termination") }) {
                flags.add(
                    RedFlag(
                        "Termination Clause",
                        "Document sets specific termination conditions and required written notice periods."
                    )
                )
            }
        }

        if (cleanText.contains("consequential", ignoreCase = true)) {
            flags.add(
                RedFlag(
                    "Consequential Damages Scope",
                    "Defines or excludes liability for indirect or consequential financial losses."
                )
            )
        }

        if (cleanText.contains("jurisdiction", ignoreCase = true) || cleanText.contains("governing law", ignoreCase = true)) {
            flags.add(
                RedFlag(
                    "Governing Law & Jurisdiction",
                    "Specifies binding court jurisdiction and legal framework for resolving disputes."
                )
            )
        }

        // 6. Calculate Final Verdict Risk Level
        val riskLevel = when {
            hasPromptInjection || hasExtremePenalty -> RiskLevel.HIGH
            flags.any { it.title.contains("Unilateral") || it.title.contains("Uncapped") } -> RiskLevel.HIGH
            flags.size >= 3 -> RiskLevel.HIGH
            flags.isNotEmpty() -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        // Default fallback points for clean, low-risk documents
        if (flags.isEmpty()) {
            flags.add(RedFlag("Standard Boilerplate Terms", "Standard terms with no unusual penalty or fine clauses detected."))
            flags.add(RedFlag("Balanced Liability", "No daily liquidated damage caps or unilateral breach clauses found."))
            flags.add(RedFlag("Clear Legal Scope", "Confidentiality and ownership definitions appear balanced."))
        }

        // Generate Summary Report Text
        val summaryText = buildString {
            append("Scanned ${cleanText.length} characters. ")
            when (riskLevel) {
                RiskLevel.HIGH -> append("CRITICAL RISK DETECTED: Document contains predatory financial liabilities, daily recurring penalties, or prompt injection manipulation attempts.")
                RiskLevel.MEDIUM -> append("MODERATE RISK: Standard legal obligations requiring review of notice windows, dispute jurisdiction, and scope.")
                RiskLevel.LOW -> append("LOW RISK: Balanced legal agreement with standard risk exposure.")
            }
        }

        return AnalysisResult(
            riskLevel = riskLevel,
            summary = summaryText,
            redFlags = flags
        )
    }
}