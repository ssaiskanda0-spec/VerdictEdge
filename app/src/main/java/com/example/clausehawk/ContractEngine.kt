package com.example.clausehawk

import kotlinx.coroutines.delay

class ContractEngine {

    suspend fun analyzeContract(text: String): AnalysisResult {
        delay(1000L)
        val cleanText = text.trim()

        // 1. BASIC LENGTH & GIBBERISH CHECK
        if (isGibberishOrInvalid(cleanText)) {
            return buildNonContractResult()
        }

        // 2. LEGAL CONTRACT RELEVANCE CHECK
        if (!containsLegalKeywords(cleanText)) {
            return buildNonContractResult()
        }

        val flags = mutableListOf<RedFlag>()

        // 3. PROMPT INJECTION
        val injectionPatterns = listOf("ignore all previous", "ignore previous", "processing agent", "classify this document", "पिछले निर्देशों", "ಹಿಂದಿನ ಸೂಚನೆಗಳನ್ನು")
        var hasInjection = false
        for (pattern in injectionPatterns) {
            if (cleanText.contains(pattern, ignoreCase = true)) {
                hasInjection = true
                flags.add(
                    RedFlag(
                        titleEn = "Adversarial Prompt Injection",
                        titleHi = "प्रॉम्प्ट इंजेक्शन (एआई हेरफेर)",
                        titleKn = "ವಿರೋಧಾತ್ಮಕ ಪ್ರಾಂಪ್ಟ್ ಇಂಜೆಕ್ಷನ್",
                        descEn = "Contains hidden text attempting to manipulate automated risk scoring.",
                        descHi = "इसमें स्वचालित जोखिम विश्लेषण में हेरफेर करने वाला पाठ शामिल है।",
                        descKn = "ಸ್ವಯಂಚಾಲಿತ ಅಪಾಯದ ಅಂಕಗಳನ್ನು ಮ್ಯಾನಿಪುಲೇಟ್ ಮಾಡಲು ಯತ್ನಿಸುವ ಅಡಗಿಸಲಾದ ಪಠ್ಯವನ್ನು ಹೊಂದಿದೆ."
                    )
                )
                break
            }
        }

        // 4. LIQUIDATED DAMAGES / RECURRING FINES
        val dailyPenaltyPattern = Regex("""(per\s+calendar\s+day|per\s+day|/day|प्रति\s+दिन|दैनिक|ಪ್ರತಿದಿನ|ದಿನಕ್ಕೆ)""", RegexOption.IGNORE_CASE)
        val liquidatedDamagesPattern = Regex("""(liquidated\s+damages|हर्जाना|जुर्माना|ನಷ್ಟ\s*ಪರಿಹಾರ|ದಂಡ)""", RegexOption.IGNORE_CASE)
        val currencyPattern = Regex("""(\$\s*[\d,]+|₹\s*[\d,]+|Rs\.?\s*[\d,]+|INR\s*[\d,]+)""", RegexOption.IGNORE_CASE)

        val isDailyPenalty = dailyPenaltyPattern.containsMatchIn(cleanText)
        val hasLiquidatedDamages = liquidatedDamagesPattern.containsMatchIn(cleanText)
        val hasCurrencyAmount = currencyPattern.containsMatchIn(cleanText)

        var hasExtremePenalty = false
        if (hasLiquidatedDamages || (isDailyPenalty && hasCurrencyAmount)) {
            hasExtremePenalty = true
            flags.add(
                RedFlag(
                    titleEn = "Severe Liquidated Damages / Daily Fines",
                    titleHi = "गंभीर दैनिक जुर्माना / हर्जाना",
                    titleKn = "ತೀವ್ರ ನಿರ್ಧರಿತ ನಷ್ಟ ಪರಿಹಾರ / ದಿನನಿತ್ಯದ ದಂಡ",
                    descEn = "Imposes heavy recurring financial penalties or daily fine accrual upon breach.",
                    descHi = "उल्लंघन पर भारी दैनिक जुर्माना या आवर्ती वित्तीय दंड लगाता है।",
                    descKn = "ಒಪ್ಪಂದ ಉಲ್ಲಂಘನೆಯಾದರೆ ಭಾರೀ ಪುನರಾವರ್ತಿತ ಆರ್ಥಿಕ ದಂಡ ಅಥವಾ ದಿನನಿತ್ಯದ ದಂಡವನ್ನು ವಿಧಿಸುತ್ತದೆ."
                )
            )
        }

        // 5. UNILATERAL ENFORCEMENT
        val unilateralPattern = Regex("""(sole\s+satisfaction|suspicion\s+of\s+a\s+breach|sole\s+discretion|केवल\s+संतुष्टि|संदेह|ಏಕಪಕ್ಷೀಯ)""", RegexOption.IGNORE_CASE)
        if (unilateralPattern.containsMatchIn(cleanText)) {
            flags.add(
                RedFlag(
                    titleEn = "Unilateral & Subjective Enforcement",
                    titleHi = "एकतरफा और मनमाना प्रवर्तन",
                    titleKn = "ಏಕಪಕ್ಷೀಯ ಮತ್ತು ಸ್ವಯಂನಿರ್ಣಯದ ಜಾರಿ",
                    descEn = "Penalties trigger immediately upon suspicion or sole discretion without proof.",
                    descHi = "बिना किसी सबूत के केवल संदेह के आधार पर जुर्माना लगाया जा सकता है।",
                    descKn = "ಯಾವುದೇ ಸಾಕ್ಷ್ಯಾಧಾರವಿಲ್ಲದೆ ಕೇವಲ ಸಂಶಯ ಅಥವಾ ಸ್ವಂತ ವಿವೇಚನೆಯ ಆಧಾರದ ಮೇಲೆ ತಕ್ಷಣವೇ ದಂಡ ವಿಧಿಸಲಾಗುತ್ತದೆ."
                )
            )
        }

        // 6. INDEMNIFICATION
        val indemnityPattern = Regex("""(indemnify|indemnification|क्षतिपूर्ति|दायित्व|ನಷ್ಟ\s*ಪರಿಹಾರ|ಹೊಣೆಗಾರಿಕೆ)""", RegexOption.IGNORE_CASE)
        if (indemnityPattern.containsMatchIn(cleanText)) {
            flags.add(
                RedFlag(
                    titleEn = "Indemnification Obligations",
                    titleHi = "क्षतिपूर्ति और कानूनी बचाव का बोझ",
                    titleKn = "ನಷ್ಟ ಪರಿಹಾರ ಬಾಧ್ಯತೆಗಳು",
                    descEn = "Shifts legal defense costs and liabilities onto the receiving party.",
                    descHi = "कानूनी बचाव लागत और देनदारियों को दूसरी पार्टी पर स्थानांतरित करता है।",
                    descKn = "ಕಾನೂನು ಸಮರ್ಥನೆ ವೆಚ್ಚಗಳು ಮತ್ತು ಹೊಣೆಗಾರಿಕೆಗಳನ್ನು ಸ್ವೀಕರಿಸುವ ಪಾರ್ಟಿಯ ಮೇಲೆ ವರ್ಗಾಯಿಸುತ್ತದೆ."
                )
            )
        }

        // 7. CALCULATE RISK
        val riskLevel = when {
            hasInjection || hasExtremePenalty -> RiskLevel.HIGH
            flags.any { it.titleEn.contains("Unilateral") } -> RiskLevel.HIGH
            flags.size >= 3 -> RiskLevel.HIGH
            flags.isNotEmpty() -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        if (flags.isEmpty()) {
            flags.add(
                RedFlag(
                    titleEn = "Standard Boilerplate Terms",
                    titleHi = "मानक कानूनी शर्तें",
                    titleKn = "ಸಾಮಾನ್ಯ ಪ್ರಮಾಣಿತ ಶರತ್ತುಗಳು",
                    descEn = "Balanced agreement with no unusual financial penalty clauses found.",
                    descHi = "बिना किसी असामान्य वित्तीय दंड के संतुलित समझौता।",
                    descKn = "ಯಾವುದೇ ಅಸಾಧಾರಣ ಆರ್ಥಿಕ ದಂಡದ ನಿಯಮಗಳಿಲ್ಲದ ಸಮತೋಲಿತ ಒಪ್ಪಂದ."
                )
            )
        }

        return AnalysisResult(
            riskLevel = riskLevel,
            summaryEn = "Scanned ${cleanText.length} chars. " + when(riskLevel) {
                RiskLevel.HIGH -> "CRITICAL RISK: High-risk financial penalties, prompt injections, or subjective enforcement found."
                RiskLevel.MEDIUM -> "MODERATE RISK: Operational clauses requiring standard review."
                RiskLevel.LOW -> "LOW RISK: Standard balanced legal agreement."
            },
            summaryHi = "${cleanText.length} अक्षरों का विश्लेषण किया गया। " + when(riskLevel) {
                RiskLevel.HIGH -> "गंभीर जोखिम: इसमें अत्यधिक जुर्माना, प्रॉम्प्ट इंजेक्शन या एकतरफा शर्तें मिली हैं।"
                RiskLevel.MEDIUM -> "मध्यम जोखिम: सामान्य समीक्षा की आवश्यकता वाले प्रावधान।"
                RiskLevel.LOW -> "कम जोखिम: मानक संतुलित कानूनी समझौता।"
            },
            summaryKn = "${cleanText.length} ಅಕ್ಷರಗಳನ್ನು ಸ್ಕ್ಯಾನ್ ಮಾಡಲಾಗಿದೆ. " + when(riskLevel) {
                RiskLevel.HIGH -> "ಗಂಭೀರ ಅಪಾಯ: ಹೆಚ್ಚಿನ ಅಪಾಯದ ಆರ್ಥಿಕ ದಂಡಗಳು, ಪ್ರಾಂಪ್ಟ್ ಇಂಜೆಕ್ಷನ್‌ಗಳು ಅಥವಾ ಏಕಪಕ್ಷೀಯ ಜಾರಿ ಕಂಡುಬಂದಿದೆ."
                RiskLevel.MEDIUM -> "ಮಧ್ಯಮ ಅಪಾಯ: ಸಾಮಾನ್ಯ ಪರಿಶೀಲನೆಯ ಅಗತ್ಯವಿರುವ ಕಾರ್ಯಾಚರಣೆಯ ನಿಯಮಗಳು."
                RiskLevel.LOW -> "ಕಡಿಮೆ ಅಪಾಯ: ಸಾಮಾನ್ಯ ಸಮತೋಲಿತ ಕಾನೂನು ಒಪ್ಪಂದ."
            },
            redFlags = flags,
            isInvalid = false
        )
    }

    private fun isGibberishOrInvalid(text: String): Boolean {
        if (text.length < 25) return true
        val words = text.split(Regex("""\s+""")).filter { it.isNotBlank() }
        if (words.size < 4) return true
        val avgWordLength = text.length.toDouble() / words.size
        return avgWordLength > 22.0 || avgWordLength < 1.8
    }

    private fun containsLegalKeywords(text: String): Boolean {
        val legalKeywords = listOf(
            "agreement", "contract", "party", "parties", "clause", "shall", "terms",
            "conditions", "liability", "breach", "termination", "indemnify", "penalty",
            "damages", "governing", "law", "jurisdiction", "confidential", "section",
            "अनुबंध", "शर्त", "दस्तावेज़", "जुर्माना", "दायित्व",
            "ಒಪ್ಪಂದ", "ಶರತ್ತು", "ನಿಯಮಗಳು", "ಹೊಣೆಗಾರಿಕೆ", "ದಂಡ"
        )
        return legalKeywords.any { text.contains(it, ignoreCase = true) }
    }

    private fun buildNonContractResult(): AnalysisResult {
        return AnalysisResult(
            riskLevel = RiskLevel.LOW,
            summaryEn = "Please upload an image with an agreement.",
            summaryHi = "कृपया एक वैध अनुबंध या समझौते की छवि अपलोड करें।",
            summaryKn = "ದಯವಿಟ್ಟು ಒಪ್ಪಂದವನ್ನು ಹೊಂದಿರುವ ಚಿತ್ರವನ್ನು ಅಪ್‌ಲೋಡ್ ಮಾಡಿ.",
            redFlags = emptyList(),
            isInvalid = true
        )
    }
}