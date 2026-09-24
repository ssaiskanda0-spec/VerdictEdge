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
    )
}

val RiskLevel.color: Color
    get() = when (this) {
        RiskLevel.HIGH -> Color(0xFFEF4444)
        RiskLevel.MEDIUM -> Color(0xFFF59E0B)
        RiskLevel.LOW -> Color(0xFF10B981)
    }

data class RedFlag(
    val titleEn: String,
    val titleHi: String,
    val titleKn: String,
    val descEn: String,
    val descHi: String,
    val descKn: String
)

data class AnalysisResult(
    val riskLevel: RiskLevel,
    val summaryEn: String,
    val summaryHi: String,
    val summaryKn: String,
    val redFlags: List<RedFlag>,
    val isInvalid: Boolean = false
)