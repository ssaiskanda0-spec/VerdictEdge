package com.example.clausehawk

import androidx.compose.ui.graphics.Color

enum class AppLanguage(val label: String) {
    ENGLISH("English"),
    HINDI("हिंदी"),
    KANNADA("ಕನ್ನಡ")
}

enum class RiskLevel(val labelEn: String, val labelHi: String, val labelKn: String) {
    LOW(labelEn = "LOW RISK", labelHi = "कम जोखिम (LOW RISK)", labelKn = "ಕಡಿಮೆ ಅಪಾಯ (LOW RISK)"),
    MEDIUM(labelEn = "MODERATE RISK", labelHi = "मध्यम जोखिम (MODERATE RISK)", labelKn = "ಮಧ್ಯಮ ಅಪಾಯ (MODERATE RISK)"),
    HIGH(labelEn = "CRITICAL RISK", labelHi = "गंभीर जोखिम (CRITICAL RISK)", labelKn = "ಗಂಭೀರ ಅಪಾಯ (CRITICAL RISK)");

    fun getLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> labelEn
        AppLanguage.HINDI -> labelHi
        AppLanguage.KANNADA -> labelKn
    }
}

val RiskLevel.color: Color
    get() = when (this) {
        RiskLevel.HIGH -> Color(0xFFEF4444)
        RiskLevel.MEDIUM -> Color(0xFFF59E0B)
        RiskLevel.LOW -> Color(0xFF10B981)
    }

fun RiskLevel.getDescription(lang: AppLanguage): String = when (lang) {
    AppLanguage.ENGLISH -> when (this) {
        RiskLevel.HIGH -> "Critical liability exposures and unilateral terms detected."
        RiskLevel.MEDIUM -> "Standard operational clauses requiring negotiation or review."
        RiskLevel.LOW -> "Standard balanced agreement with minimal legal exposure."
    }
    AppLanguage.HINDI -> when (this) {
        RiskLevel.HIGH -> "गंभीर कानूनी देनदारियां और एकतरफा शर्तें पाई गईं।"
        RiskLevel.MEDIUM -> "मानक परिचालन शर्तें जिन्हें समीक्षा की आवश्यकता है।"
        RiskLevel.LOW -> "न्यूनतम कानूनी जोखिम वाला संतुलित समझौता।"
    }
    AppLanguage.KANNADA -> when (this) {
        RiskLevel.HIGH -> "ಗಂಭೀರ ಹೊಣೆಗಾರಿಕೆಯ ಅಪಾಯಗಳು ಮತ್ತು ಏಕಪಕ್ಷೀಯ ನಿಯಮಗಳು ಪತ್ತೆಯಾಗಿವೆ."
        RiskLevel.MEDIUM -> "ಸಮೀಕ್ಷೆ ಅಥವಾ ಸಂಧಾನದ ಅಗತ್ಯವಿರುವ ಸಾಮಾನ್ಯ ಕಾರ್ಯಾಚರಣೆಯ ಶರತ್ತುಗಳು."
        RiskLevel.LOW -> "ಕನಿಷ್ಠ ಕಾನೂನು ಅಪಾಯವನ್ನು ಹೊಂದಿರುವ ಸಮತೋಲಿತ ಸಾಮಾನ್ಯ ಒಪ್ಪಂದ."
    }
}

data class RedFlag(
    val titleEn: String,
    val titleHi: String,
    val titleKn: String,
    val descEn: String,
    val descHi: String,
    val descKn: String
) {
    fun getTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> titleEn
        AppLanguage.HINDI -> titleHi
        AppLanguage.KANNADA -> titleKn
    }

    fun getDesc(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> descEn
        AppLanguage.HINDI -> descHi
        AppLanguage.KANNADA -> descKn
    }
}

data class AnalysisResult(
    val riskLevel: RiskLevel,
    val summaryEn: String,
    val summaryHi: String,
    val summaryKn: String,
    val redFlags: List<RedFlag>,
    val isInvalid: Boolean = false
) {
    fun getSummary(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> summaryEn
        AppLanguage.HINDI -> summaryHi
        AppLanguage.KANNADA -> summaryKn
    }
}