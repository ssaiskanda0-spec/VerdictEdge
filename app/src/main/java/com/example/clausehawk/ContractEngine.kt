package com.example.clausehawk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContractEngine {

    suspend fun analyzeContract(text: String): AnalysisResult = withContext(Dispatchers.Default) {
        val trimmed = text.trim()
        val lowerText = trimmed.lowercase()

        // 1. Check for Gibberish / Invalid Text
        if (trimmed.length < 15 || !containsMeaningfulWords(trimmed)) {
            return@withContext AnalysisResult(
                riskLevel = RiskLevel.LOW,
                summaryEn = "Invalid input or gibberish text detected. Please provide clear legal or contract text.",
                summaryHi = "अमान्य इनपुट या निरर्थक पाठ पाया गया। कृपया स्पष्ट कानूनी या अनुबंध पाठ प्रदान करें।",
                summaryKn = "ಅಮಾನ್ಯ ಇನ್‌ಪುಟ್ ಅಥವಾ ಅರ್ಥವಿಲ್ಲದ ಪಠ್ಯ ಪತ್ತೆಯಾಗಿದೆ. ದಯವಿಟ್ಟು ಸ್ಪಷ್ಟ ಕಾನೂನು ಪಠ್ಯವನ್ನು ಒದಗಿಸಿ.",
                redFlags = emptyList(),
                isInvalid = true
            )
        }

        val redFlags = mutableListOf<RedFlag>()

        // Keywords in EN, HI, KN
        val highRiskKeywords = listOf(
            "indemnify", "indemnity", "unilateral", "terminate without cause",
            "non-compete", "arbitration", "penalty", "liable for all damages",
            "exclusive jurisdiction", "वाध्य", "हरजाना", "ಬಂಧನಕಾರಿ", "ದಂಡ"
        )

        val mediumRiskKeywords = listOf(
            "auto-renew", "automatic renewal", "confidentiality", "intellectual property",
            "governing law", "notice period", "amendment", "गोपनीयता", "ಖಾಸಗಿತನ"
        )

        var highCount = 0
        var mediumCount = 0

        for (keyword in highRiskKeywords) {
            if (lowerText.contains(keyword)) highCount++
        }

        for (keyword in mediumRiskKeywords) {
            if (lowerText.contains(keyword)) mediumCount++
        }

        // Clause Flag Matching
        if (lowerText.contains("indemnif") || lowerText.contains("liable") || lowerText.contains("हरजाना") || lowerText.contains("ದಂಡ")) {
            redFlags.add(
                RedFlag(
                    titleEn = "Unbalanced Indemnification & Liability",
                    titleHi = "असंतुलित क्षतिपूर्ति और देनदारी",
                    titleKn = "ಅಸಮತೋಲಿತ ನಷ್ಟ ಪರಿಹಾರ ಮತ್ತು ಜವಾಬ್ದಾರಿ",
                    descEn = "The agreement contains terms shifting legal liability or obligation to protect the other party unconditionally.",
                    descHi = "समझौते में बिना शर्त दूसरी पार्टी की रक्षा करने या कानूनी दायित्व स्थानांतरित करने की शर्तें शामिल हैं।",
                    descKn = "ಒಪ್ಪಂದವು ಇತರ ಪಕ್ಷವನ್ನು ಶರತ್ತಿಲ್ಲದೆ ರಕ್ಷಿಸುವ ಅಥವಾ ಕಾನೂನು ಜವಾಬ್ದಾರಿಯನ್ನು ವರ್ಗಾಯಿಸುವ ನಿಯಮಗಳನ್ನು ಹೊಂದಿದೆ."
                )
            )
        }

        if (lowerText.contains("terminate") || lowerText.contains("cancellation") || lowerText.contains("रद्द")) {
            redFlags.add(
                RedFlag(
                    titleEn = "Unilateral Termination Rights",
                    titleHi = "एकपक्षीय समाप्ति अधिकार",
                    titleKn = "ಏಕಪಕ್ಷೀಯ ರದ್ದತಿ ಹಕ್ಕುಗಳು",
                    descEn = "Provisions allow contract cancellation without mutual agreement or explicit cause.",
                    descHi = "प्रावधान बिना आपसी सहमति या स्पष्ट कारण के अनुबंध रद्द करने की अनुमति देते हैं।",
                    descKn = "ಪರಸ್ಪರ ಸಮ್ಮತಿ ಅಥವಾ ನಿರ್ದಿಷ್ಟ ಕಾರಣವಿಲ್ಲದೆ ಒಪ್ಪಂದವನ್ನು ರದ್ದುಗೊಳಿಸಲು ಈ ನಿಯಮಗಳು ಅನುಮತಿಸುತ್ತವೆ."
                )
            )
        }

        if (lowerText.contains("non-compete") || lowerText.contains("exclusiv")) {
            redFlags.add(
                RedFlag(
                    titleEn = "Restrictive Covenants",
                    titleHi = "प्रतिबंधात्मक शर्तें",
                    titleKn = "ನಿಯಂತ್ರಣ ಶರತ್ತುಗಳು",
                    descEn = "Contains non-compete or strict exclusivity restrictions limiting future operational scope.",
                    descHi = "गैर-प्रतिस्पर्धा या सख्त विशिष्टता प्रतिबंध शामिल हैं जो भविष्य के काम को सीमित करते हैं।",
                    descKn = "ಭವಿಷ್ಯದ ಕೆಲಸಗಳನ್ನು ಮಿತಿಗೊಳಿಸುವ ಸ್ಪರ್ಧಾತ್ಮಕವಲ್ಲದ ಅಥವಾ ಕಟ್ಟುನಿಟ್ಟಾದ ನಿರ್ಬಂಧಗಳನ್ನು ಹೊಂದಿದೆ."
                )
            )
        }

        if (lowerText.contains("auto-renew") || lowerText.contains("renew")) {
            redFlags.add(
                RedFlag(
                    titleEn = "Automatic Renewal Clause",
                    titleHi = "ऑटोमैटिक नवीनीकरण क्लॉज",
                    titleKn = "ಸ್ವಯಂಚಾಲಿತ ನವೀಕರಣ ಶರತ್ತು",
                    descEn = "The contract automatically extends unless explicit cancellation notice is served in time.",
                    descHi = "समय पर रद्द करने की सूचना न देने पर अनुबंध अपने आप आगे बढ़ जाएगा।",
                    descKn = "ಸಮಯಕ್ಕೆ ಸರಿಯಾಗಿ ರದ್ದತಿ ಸೂಚನೆ ನೀಡದಿದ್ದರೆ ಒಪ್ಪಂದವು ಸ್ವಯಂಚಾಲಿತವಾಗಿ ವಿಸ್ತರಿಸಲ್ಪಡುತ್ತದೆ."
                )
            )
        }

        if (redFlags.isEmpty()) {
            redFlags.add(
                RedFlag(
                    titleEn = "Standard Terms Identified",
                    titleHi = "मानक शर्तें पाई गईं",
                    titleKn = "ಸಾಮಾನ್ಯ ನಿಯಮಗಳನ್ನು ಗುರುತಿಸಲಾಗಿದೆ",
                    descEn = "General contract terms detected with no major unilateral liability restrictions.",
                    descHi = "बिना किसी बड़े एकपक्षीय दायित्व के सामान्य अनुबंध शर्तें पाई गईं।",
                    descKn = "ಯಾವುದೇ ಪ್ರಮುಖ ಏಕಪಕ್ಷೀಯ ಜವಾಬ್ದಾರಿ ಇಲ್ಲದೆ ಸಾಮಾನ್ಯ ಒಪ್ಪಂದದ ನಿಯಮಗಳು ಕಂಡುಬಂದಿವೆ."
                )
            )
        }

        val riskLevel = when {
            highCount >= 2 || redFlags.size >= 3 -> RiskLevel.HIGH
            highCount == 1 || mediumCount >= 1 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        AnalysisResult(
            riskLevel = riskLevel,
            summaryEn = when (riskLevel) {
                RiskLevel.HIGH -> "High-risk terms identified. Review indemnity, liability caps, and termination rights carefully."
                RiskLevel.MEDIUM -> "Moderate terms detected. Standard clauses present; ensure notice periods are acceptable."
                RiskLevel.LOW -> "Standard legal terms detected. Agreement appears balanced."
            },
            summaryHi = when (riskLevel) {
                RiskLevel.HIGH -> "उच्च जोखिम वाली शर्तें पाई गईं। क्षतिपूर्ति और समाप्ति अधिकारों की ध्यान से समीक्षा करें।"
                RiskLevel.MEDIUM -> "मध्यम शर्तें पाई गईं। सुनिश्चित करें कि नोटिस अवधि स्वीकार्य है।"
                RiskLevel.LOW -> "मानक कानूनी शर्तें पाई गईं। समझौता संतुलित प्रतीत होता है।"
            },
            summaryKn = when (riskLevel) {
                RiskLevel.HIGH -> "ಹೆಚ್ಚಿನ ಅಪಾಯದ ನಿಯಮಗಳು ಕಂಡುಬಂದಿವೆ. ಪರಿಹಾರ ಮತ್ತು ರದ್ದತಿ ಹಕ್ಕುಗಳನ್ನು ಎಚ್ಚರಿಕೆಯಿಂದ ಪರಿಶೀಲಿಸಿ."
                RiskLevel.MEDIUM -> "ಮಧ್ಯಮ ನಿಯಮಗಳು ಕಂಡುಬಂದಿವೆ. ಸೂಚನಾ ಅವಧಿ ಸ್ವೀಕಾರಾರ್ಹವಾಗಿದೆ ಎಂದು ಖಚಿತಪಡಿಸಿಕೊಳ್ಳಿ."
                RiskLevel.LOW -> "ಸಾಮಾನ್ಯ ಕಾನೂನು ನಿಯಮಗಳು ಕಂಡುಬಂದಿವೆ. ಒಪ್ಪಂದವು ಸಮತೋಲಿತವಾಗಿದೆ."
            },
            redFlags = redFlags,
            isInvalid = false
        )
    }

    private fun containsMeaningfulWords(text: String): Boolean {
        val vowelsAndConsonants = Regex("[a-zA-Z\\u0900-\\u097F\\u0C80-\\u0CFF]")
        return vowelsAndConsonants.containsMatchIn(text)
    }
}