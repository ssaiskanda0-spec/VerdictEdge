package com.example.clausehawk

import androidx.compose.ui.graphics.Color

enum class RiskLevel(
    val labelEn: String,
    val labelHi: String,
    val labelKn: String,
    val descEn: String,
    val descHi: String,
    val descKn: String
) {
    LOW(
        labelEn = "LOW RISK",
        labelHi = "कम जोखिम",
        labelKn = "ಕಡಿಮೆ ಅಪಾಯ",
        descEn = "Standard balanced agreement with minimal legal exposure.",
        descHi = "न्यूनतम कानूनी जोखिम के साथ मानक संतुलित समझौता।",
        descKn = "ಕನಿಷ್ಠ ಕಾನೂನು ಅಪಾಯದೊಂದಿಗೆ ಸಾಮಾನ್ಯ ಸಮತೋಲಿತ ಒಪ್ಪಂದ."
    ),
    MEDIUM(
        labelEn = "MODERATE RISK",
        labelHi = "मध्यम जोखिम",
        labelKn = "ಮಧ್ಯಮ ಅಪಾಯ",
        descEn = "Standard operational clauses requiring negotiation or review.",
        descHi = "मानक परिचालन शर्तें जिनके लिए बातचीत या समीक्षा की आवश्यकता है।",
        descKn = "ಚರ್ಚೆ ಅಥವಾ ಪರಿಶೀಲನೆ ಅಗತ್ಯವಿರುವ ಸಾಮಾನ್ಯ ಕಾರ್ಯಾಚರಣೆಯ ನಿಯಮಗಳು."
    ),
    HIGH(
        labelEn = "CRITICAL RISK",
        labelHi = "गंभीर जोखिम",
        labelKn = "ಹೆಚ್ಚಿನ ಅಪಾಯ",
        descEn = "Critical liability exposures and unilateral terms detected.",
        descHi = "गंभीर देनदारी जोखिम और एकपक्षीय शर्तें पाई गईं।",
        descKn = "ಗಂಭೀರ ಜವಾಬ್ದಾರಿ ಅಪಾಯಗಳು ಮತ್ತು ಏಕಪಕ್ಷೀಯ ನಿಯಮಗಳನ್ನು ಗುರುತಿಸಲಾಗಿದೆ."
    ),
    INVALID(
        labelEn = "NON-CONTRACT / UNREADABLE",
        labelHi = "गैर-अनुबंध / अपठनीय",
        labelKn = "ಒಪ್ಪಂದವಲ್ಲದ / ಅಪಠ್ಯ",
        descEn = "No legal agreement terms or valid contract text detected.",
        descHi = "दस्तावेज़ में कोई कानूनी समझौता या शर्तें नहीं मिलीं।",
        descKn = "ದಾಖಲೆಯಲ್ಲಿ ಯಾವುದೇ ಕಾನೂನು ಒಪ್ಪಂದದ ನಿಯಮಗಳು ಕಂಡುಬಂದಿಲ್ಲ."
    )
}

val RiskLevel.color: Color
    get() = when (this) {
        RiskLevel.HIGH -> Color(0xFFEF4444)
        RiskLevel.MEDIUM -> Color(0xFFF59E0B)
        RiskLevel.LOW -> Color(0xFF10B981)
        RiskLevel.INVALID -> Color(0xFF6B7280)
    }

data class RedFlag(
    val titleEn: String,
    val titleHi: String,
    val titleKn: String,
    val descEn: String,
    val descHi: String,
    val descKn: String,
    val quoteSnippet: String = ""
)

data class DeadlineObligation(
    val timeframe: String,
    val obligationEn: String,
    val obligationHi: String,
    val obligationKn: String,
    val quoteSnippet: String = ""
)

data class FinancialExposure(
    val titleEn: String,
    val titleHi: String,
    val titleKn: String,
    val amountOrCost: String,
    val descriptionEn: String,
    val descriptionHi: String,
    val descriptionKn: String,
    val quoteSnippet: String = ""
)

data class StatutoryVoidability(
    val actSection: String,
    val titleEn: String,
    val titleHi: String,
    val titleKn: String,
    val legalReasonEn: String,
    val legalReasonHi: String,
    val legalReasonKn: String,
    val quoteSnippet: String = "",
    val status: String = "POTENTIALLY VOID"
)

data class ProblemSolutionBreakdown(
    val originalSnippet: String,
    val problemEn: String,
    val problemHi: String,
    val problemKn: String,
    val solutionEn: String,
    val solutionHi: String,
    val solutionKn: String,
    val counterOfferDraft: String
)

data class AmbiguityTerm(
    val phrase: String,
    val explanationEn: String,
    val explanationHi: String,
    val explanationKn: String,
    val quoteSnippet: String = ""
)

data class DealbreakerMatch(
    val ruleKeyword: String,
    val matchedContext: String
)

data class PreSigningCheckItem(
    val id: Int,
    val taskEn: String,
    val taskHi: String,
    val taskKn: String,
    var isResolved: Boolean = false
)

data class AnalysisResult(
    val riskLevel: RiskLevel,
    val summaryEn: String,
    val summaryHi: String,
    val summaryKn: String,
    val redFlags: List<RedFlag>,
    val deadlines: List<DeadlineObligation> = emptyList(),
    val financialExposures: List<FinancialExposure> = emptyList(),
    val statutoryVoidabilities: List<StatutoryVoidability> = emptyList(),
    val clauseBreakdowns: List<ProblemSolutionBreakdown> = emptyList(),
    val ambiguities: List<AmbiguityTerm> = emptyList(),
    val dealbreakerMatches: List<DealbreakerMatch> = emptyList(),
    val preSigningChecklist: List<PreSigningCheckItem> = emptyList(),
    val isInvalid: Boolean = false
)