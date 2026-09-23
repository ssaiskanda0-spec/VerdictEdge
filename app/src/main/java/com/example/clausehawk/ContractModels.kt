package com.example.clausehawk

import androidx.compose.ui.graphics.Color

enum class RiskLevel(val label: String) {
    LOW(label = "LOW RISK"),
    MEDIUM(label = "MODERATE RISK"),
    HIGH(label = "CRITICAL RISK")
}

val RiskLevel.color: Color
    get() = when (this) {
        RiskLevel.HIGH -> Color(0xFFEF4444)
        RiskLevel.MEDIUM -> Color(0xFFF59E0B)
        RiskLevel.LOW -> Color(0xFF10B981)
    }

val RiskLevel.description: String
    get() = when (this) {
        RiskLevel.HIGH -> "Critical liability exposures and unilateral terms detected."
        RiskLevel.MEDIUM -> "Standard operational clauses requiring negotiation or review."
        RiskLevel.LOW -> "Standard balanced agreement with minimal legal exposure."
    }

data class RedFlag(
    val title: String,
    val description: String
)

data class AnalysisResult(
    val riskLevel: RiskLevel,
    val summary: String,
    val redFlags: List<RedFlag>
)