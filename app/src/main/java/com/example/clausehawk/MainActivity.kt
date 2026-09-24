package com.example.clausehawk

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

enum class AppState {
    INIT_LOADING,
    INPUT,
    PROCESSING_LOADING,
    RESULTS
}

enum class Language {
    ENGLISH,
    HINDI,
    KANNADA
}

data class HistoryRecord(
    val timestamp: Long,
    val riskName: String,
    val title: String,
    val snippet: String,
    val fullText: String
)

private fun createTempImageUri(context: Context): Uri {
    val tempFile = File.createTempFile("scan_doc_", ".jpg", context.cacheDir).apply {
        createNewFile()
        deleteOnExit()
    }
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
}

fun formatCompactAmount(raw: String): String {
    val trimmed = raw.trim()
    var formatted = trimmed.replace(".00", "")
    if (formatted.length > 10) {
        formatted = formatted.replace(" USD", "", ignoreCase = true)
    }
    return formatted
}

fun extractCleanTitle(fullText: String): String {
    val lines = fullText.lines()
    for (line in lines) {
        val cleanLine = line.replace(Regex("--- \\[Document \\d+\\] ---"), "").trim()
        if (cleanLine.isNotBlank() && !cleanLine.startsWith("---") && cleanLine.length >= 3) {
            return cleanLine.take(45).trim()
        }
    }
    return "Legal Document Analysis"
}

fun buildFullAudioSummary(result: AnalysisResult, lang: Language): String {
    val builder = StringBuilder()

    val riskTitle = when (lang) {
        Language.ENGLISH -> "VerdictEdge Risk Analysis Report."
        Language.HINDI -> "वर्डिक्टएज जोखिम विश्लेषण रिपोर्ट।"
        Language.KANNADA -> "ವರ್ಡಿಕ್ಟ್-ಎಡ್ಜ್ ಅಪಾಯದ ವಿಶ್ಲೇಷಣೆ ವರದಿ."
    }
    builder.append(riskTitle).append(" ")

    val overallSummary = when (lang) {
        Language.ENGLISH -> "Overall Assessment: ${result.riskLevel.labelEn}. ${result.summaryEn}"
        Language.HINDI -> "कुल मूल्यांकन: ${result.riskLevel.labelHi}। ${result.summaryHi}"
        Language.KANNADA -> "ಒಟ್ಟು ಮೌಲ್ಯಮಾಪನ: ${result.riskLevel.labelKn}. ${result.summaryKn}"
    }
    builder.append(overallSummary).append(" ")

    if (result.statutoryVoidabilities.isNotEmpty()) {
        val statHeader = when (lang) {
            Language.ENGLISH -> "Warning: ${result.statutoryVoidabilities.size} statutory voidability issues detected under Indian Law."
            Language.HINDI -> "चेतावनी: भारतीय कानून के तहत ${result.statutoryVoidabilities.size} वैधानिक अमान्यता के मामले मिले।"
            Language.KANNADA -> "ಎಚ್ಚರಿಕೆ: ಭಾರತೀಯ ಕಾನೂನಿನಡಿ ${result.statutoryVoidabilities.size} ಶಾಸನಬದ್ಧ ಅಮಾನ್ಯತೆಯ ವಿಷಯಗಳು ಕಂಡುಬಂದಿವೆ."
        }
        builder.append(statHeader).append(" ")
        result.statutoryVoidabilities.forEach { stat ->
            val statReason = when (lang) {
                Language.ENGLISH -> "${stat.actSection}: ${stat.titleEn}. ${stat.legalReasonEn}"
                Language.HINDI -> "${stat.actSection}: ${stat.titleHi}। ${stat.legalReasonHi}"
                Language.KANNADA -> "${stat.actSection}: ${stat.titleKn}. ${stat.legalReasonKn}"
            }
            builder.append(statReason).append(" ")
        }
    }

    if (result.clauseBreakdowns.isNotEmpty()) {
        val vulnHeader = when (lang) {
            Language.ENGLISH -> "Key Vulnerabilities and Recommendations:"
            Language.HINDI -> "मुख्य कमियां और सिफारिशें:"
            Language.KANNADA -> "ಪ್ರಮುಖ ಲೋಪದೋಷಗಳು ಮತ್ತು ಶಿಫಾರಸುಗಳು:"
        }
        builder.append(vulnHeader).append(" ")
        result.clauseBreakdowns.take(3).forEach { cb ->
            val itemText = when (lang) {
                Language.ENGLISH -> "Problem: ${cb.problemEn}. Solution: ${cb.solutionEn}"
                Language.HINDI -> "समस्या: ${cb.problemHi}। समाधान: ${cb.solutionHi}"
                Language.KANNADA -> "ಸಮಸ್ಯೆ: ${cb.problemKn}. ಪರಿಹಾರ: ${cb.solutionKn}"
            }
            builder.append(itemText).append(" ")
        }
    }

    if (result.financialExposures.isNotEmpty()) {
        val fin = result.financialExposures.first()
        val finText = when (lang) {
            Language.ENGLISH -> "Financial Exposure Note: ${fin.titleEn}, Amount: ${fin.amountOrCost}."
            Language.HINDI -> "वित्तीय जोखिम नोट: ${fin.titleHi}, राशि: ${fin.amountOrCost}।"
            Language.KANNADA -> "ಹಣಕಾಸು ಅಪಾಯ ಟಿಪ್ಪಣಿ: ${fin.titleKn}, ಮೊತ್ತ: ${fin.amountOrCost}."
        }
        builder.append(finText)
    }

    return builder.toString()
}

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        tts = TextToSpeech(this, this)

        setContent {
            OfficialTheme {
                ClauseHawkApp(
                    onSpeakText = { text -> speakOut(text) },
                    onStopAudio = { tts?.stop() },
                    onExportPdf = { result, contractText, lang -> generatePdfReport(this, result, contractText, lang) }
                )
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            isTtsReady = true
        }
    }

    private fun speakOut(text: String) {
        if (isTtsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ClauseHawkTTS")
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
fun ClauseHawkApp(
    onSpeakText: (String) -> Unit,
    onStopAudio: () -> Unit,
    onExportPdf: (AnalysisResult, String, Language) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val ocrManager = remember { OcrManager(context) }
    val contractEngine = remember { ContractEngine() }

    var appState by remember { mutableStateOf(AppState.INIT_LOADING) }
    var contractText by remember { mutableStateOf("") }
    var dealbreakerInput by remember { mutableStateOf("") }
    var analysisResult by remember { mutableStateOf<AnalysisResult?>(null) }
    var isOcrExtracting by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf(Language.ENGLISH) }
    var processingProgress by remember { mutableStateOf("Scanning text...") }
    var isAudioPlaying by remember { mutableStateOf(false) }

    var selectedDictWord by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showHistoryDrawer by remember { mutableStateOf(false) }
    var historyItems by remember { mutableStateOf(listOf<HistoryRecord>()) }

    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempPhotoUri != null) {
            isOcrExtracting = true
            ocrManager.processImageFromUri(
                uri = tempPhotoUri!!,
                onSuccess = { extractedText ->
                    isOcrExtracting = false
                    if (extractedText.isNotBlank()) contractText = extractedText
                    else Toast.makeText(context, "No text detected", Toast.LENGTH_SHORT).show()
                },
                onError = {
                    isOcrExtracting = false
                    Toast.makeText(context, "OCR failed", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    val multiFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            isOcrExtracting = true
            coroutineScope.launch {
                ocrManager.processMultipleUris(
                    uris = uris,
                    onProgress = { current, total -> processingProgress = "Processing file $current of $total..." },
                    onComplete = { combinedText ->
                        isOcrExtracting = false
                        contractText = combinedText
                    },
                    onError = {
                        isOcrExtracting = false
                        Toast.makeText(context, "Failed to parse files", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    val launchFullResCamera = {
        try {
            val uri = createTempImageUri(context)
            tempPhotoUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to open camera", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) launchFullResCamera() else Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
    }

    val loadHistoryRecords = {
        val prefs = context.getSharedPreferences("contract_history_store", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("history_json", "[]") ?: "[]"
        val list = mutableListOf<HistoryRecord>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val fullText = obj.optString("fullText")
                val rawSnippet = obj.optString("snippet")
                val cleanSnippet = rawSnippet.replace(Regex("--- \\[Document \\d+\\] ---"), "").trim()
                val cleanTitle = extractCleanTitle(fullText)

                list.add(
                    HistoryRecord(
                        timestamp = obj.optLong("timestamp"),
                        riskName = obj.optString("riskName"),
                        title = cleanTitle,
                        snippet = if (cleanSnippet.isNotBlank()) cleanSnippet else fullText.take(100),
                        fullText = fullText
                    )
                )
            }
        } catch (e: Exception) { }
        historyItems = list.reversed()
    }

    if (appState == AppState.INIT_LOADING) {
        LaunchedEffect(Unit) {
            delay(1200L)
            appState = AppState.INPUT
        }

        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF000000)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(modifier = Modifier.size(42.dp), color = Color.White, strokeWidth = 3.dp)
                Spacer(modifier = Modifier.height(28.dp))
                Text(text = "VerdictEdge Neural Engine", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Air-gapped local legal environment ready", fontSize = 13.sp, color = Color(0xFFA0A0A0))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        if (appState == AppState.INPUT) {
            Surface(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), color = Color(0xFF000000)) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(Color(0xFF141414))
                                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(50.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isOcrExtracting) processingProgress else "Local Engine Active",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Button(
                                onClick = {
                                    loadHistoryRecords()
                                    showHistoryDrawer = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF141414)),
                                border = BorderStroke(1.dp, Color(0xFF333333)),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("🕒 History", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Text(text = "VerdictEdge", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text(text = "Multi-file Legal Risk & Statutory Analyzer", fontSize = 13.sp, color = Color(0xFFA0A0A0))

                        Spacer(modifier = Modifier.height(18.dp))

                        OutlinedTextField(
                            value = contractText,
                            onValueChange = { contractText = it },
                            placeholder = { Text("Paste contract text or scan legal documents...", color = Color(0xFF666666), fontSize = 14.sp) },
                            modifier = Modifier.fillMaxWidth().height(170.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF121212),
                                unfocusedContainerColor = Color(0xFF0A0A0A),
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color(0xFF2A2A2A),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = dealbreakerInput,
                            onValueChange = { dealbreakerInput = it },
                            placeholder = { Text("Set Dealbreaker Rules (e.g. non-compete, 60 days notice)", color = Color(0xFF666666), fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF121212),
                                unfocusedContainerColor = Color(0xFF0A0A0A),
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color(0xFF2A2A2A),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                    if (hasPermission) launchFullResCamera() else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF141414), contentColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFF333333))
                            ) {
                                Text(text = "📷 Camera", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }

                            Button(
                                onClick = { multiFileLauncher.launch("*/*") },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF141414), contentColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFF333333))
                            ) {
                                Text(text = "📂 Scan Files/PDFs", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (contractText.isNotBlank()) {
                                appState = AppState.PROCESSING_LOADING
                            }
                        },
                        enabled = contractText.isNotBlank() && !isOcrExtracting,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Text(text = "Analyze Contract Risk", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }

        if (appState == AppState.PROCESSING_LOADING) {
            LaunchedEffect(Unit) {
                coroutineScope.launch {
                    val rules = dealbreakerInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    val result = contractEngine.analyzeContract(contractText, rules)
                    analysisResult = result

                    val prefs = context.getSharedPreferences("contract_history_store", Context.MODE_PRIVATE)
                    val jsonStr = prefs.getString("history_json", "[]") ?: "[]"
                    try {
                        val array = JSONArray(jsonStr)
                        val cleanTitle = extractCleanTitle(contractText)
                        val cleanText = contractText.replace(Regex("--- \\[Document \\d+\\] ---"), "").trim()
                        val snippetText = if (cleanText.length > 90) cleanText.take(90) + "..." else cleanText

                        val newObj = JSONObject().apply {
                            put("timestamp", System.currentTimeMillis())
                            put("riskName", result.riskLevel.name)
                            put("title", cleanTitle)
                            put("snippet", snippetText)
                            put("fullText", contractText)
                        }
                        array.put(newObj)
                        prefs.edit().putString("history_json", array.toString()).apply()
                    } catch (e: Exception) { }

                    appState = AppState.RESULTS
                }
            }

            Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF000000)) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(44.dp), color = Color.White, strokeWidth = 3.5.dp)
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(text = "Evaluating Statutory & Risk Rules...", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Analyzing Discretions, Financial Exposures & Counter-Clauses", fontSize = 12.sp, color = Color(0xFFA0A0A0))
                }
            }
        }

        if (appState == AppState.RESULTS && analysisResult != null) {
            val result = analysisResult!!
            val scrollState = rememberScrollState()

            val checklistStates = remember {
                mutableStateListOf(*result.preSigningChecklist.toTypedArray())
            }

            Surface(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), color = Color(0xFF000000)) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp).verticalScroll(scrollState)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (selectedLanguage) {
                                Language.ENGLISH -> "Analysis Report"
                                Language.HINDI -> "विश्लेषण रिपोर्ट"
                                Language.KANNADA -> "ವಿಶ್ಲೇಷಣೆ ವರದಿ"
                            },
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Button(
                            onClick = { onExportPdf(result, contractText, selectedLanguage) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
                            border = BorderStroke(1.dp, Color(0xFF404040)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = when (selectedLanguage) {
                                    Language.ENGLISH -> "📄 Export PDF"
                                    Language.HINDI -> "📄 पीडीएफ निर्यात"
                                    Language.KANNADA -> "📄 PDF ರಫ್ತು"
                                },
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF121212))
                            .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(12.dp))
                            .padding(4.dp)
                    ) {
                        Language.values().forEach { lang ->
                            val isSelected = selectedLanguage == lang
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color.White else Color.Transparent)
                                    .clickable { selectedLanguage = lang }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (lang) { Language.ENGLISH -> "English" ; Language.HINDI -> "हिंदी" ; Language.KANNADA -> "ಕನ್ನಡ" },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else Color(0xFFA0A0A0)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF333333), RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(result.riskLevel.color))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when (selectedLanguage) {
                                        Language.ENGLISH -> result.riskLevel.labelEn
                                        Language.HINDI -> result.riskLevel.labelHi
                                        Language.KANNADA -> result.riskLevel.labelKn
                                    },
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = when (selectedLanguage) {
                                    Language.ENGLISH -> result.summaryEn
                                    Language.HINDI -> result.summaryHi
                                    Language.KANNADA -> result.summaryKn
                                },
                                fontSize = 13.sp,
                                color = Color(0xFFCCCCCC)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when (selectedLanguage) {
                                        Language.ENGLISH -> "🔊 Plain-Language Audio Summary"
                                        Language.HINDI -> "🔊 सरल भाषा ऑडियो सारांश"
                                        Language.KANNADA -> "🔊 ಸರಳ ಭಾಷೆಯ ಧ್ವನಿ ಸಾರಾಂಶ"
                                    },
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White
                                )
                                Text(
                                    text = when (selectedLanguage) {
                                        Language.ENGLISH -> "Listen to full spoken risk report"
                                        Language.HINDI -> "पूर्ण बोली जाने वाली जोखिम रिपोर्ट सुनें"
                                        Language.KANNADA -> "ಸಂಪೂರ್ಣ ಧ್ವನಿ ವರದಿಯನ್ನು ಆಲಿಸಿ"
                                    },
                                    fontSize = 11.sp, color = Color(0xFFA0A0A0)
                                )
                            }
                            Button(
                                onClick = {
                                    if (isAudioPlaying) {
                                        onStopAudio()
                                        isAudioPlaying = false
                                    } else {
                                        val fullSpeech = buildFullAudioSummary(result, selectedLanguage)
                                        onSpeakText(fullSpeech)
                                        isAudioPlaying = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isAudioPlaying) Color(0xFF333333) else Color.White),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isAudioPlaying) {
                                        when (selectedLanguage) { Language.ENGLISH -> "Stop"; Language.HINDI -> "रोकें"; Language.KANNADA -> "ನಿಲ್ಲಿಸಿ" }
                                    } else {
                                        when (selectedLanguage) { Language.ENGLISH -> "Play"; Language.HINDI -> "चलाएं"; Language.KANNADA -> "ಪ್ಲೇ" }
                                    },
                                    fontSize = 12.sp, color = if (isAudioPlaying) Color.White else Color.Black, fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (result.statutoryVoidabilities.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = when (selectedLanguage) {
                                Language.ENGLISH -> "⚖️ Statutory Voidability Warnings (Indian Law)"
                                Language.HINDI -> "⚖️ वैधानिक अमान्यता चेतावनी (भारतीय कानून)"
                                Language.KANNADA -> "⚖️ ಶಾಸನಬದ್ಧ ಅಮಾನ್ಯತೆಯ ಎಚ್ಚರಿಕೆಗಳು (ಭಾರತೀಯ ಕಾನೂನು)"
                            },
                            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        result.statutoryVoidabilities.forEach { stat ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color(0xFF444444), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF181818))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "${stat.actSection}: ${when (selectedLanguage) { Language.ENGLISH -> stat.titleEn; Language.HINDI -> stat.titleHi; Language.KANNADA -> stat.titleKn }}",
                                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = when (selectedLanguage) { Language.ENGLISH -> stat.legalReasonEn; Language.HINDI -> stat.legalReasonHi; Language.KANNADA -> stat.legalReasonKn },
                                        fontSize = 12.sp, color = Color(0xFFCCCCCC)
                                    )
                                    if (stat.quoteSnippet.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            color = Color(0xFF0A0A0A),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(6.dp))
                                        ) {
                                            Text(
                                                text = "“Quoted Clause: ${stat.quoteSnippet}”",
                                                fontSize = 11.sp, color = Color(0xFFA0A0A0), modifier = Modifier.padding(8.dp), lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (result.clauseBreakdowns.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = when (selectedLanguage) {
                                Language.ENGLISH -> "💡 Vulnerabilities & Counter-Offer Guidance"
                                Language.HINDI -> "💡 कमियां और जवाबी प्रस्ताव मार्गदर्शन"
                                Language.KANNADA -> "💡 ಲೋಪದೋಷಗಳು ಮತ್ತು ಪ್ರತಿಸಲ್ಲಿಕೆ ಮಾರ್ಗದರ್ಶನ"
                            },
                            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        result.clauseBreakdowns.forEach { cb ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    if (cb.originalSnippet.isNotBlank()) {
                                        Surface(
                                            color = Color(0xFF0A0A0A), shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(6.dp))
                                        ) {
                                            Text(text = "“Quoted Clause: ${cb.originalSnippet}”", fontSize = 11.sp, color = Color(0xFFCCCCCC), modifier = Modifier.padding(8.dp), lineHeight = 16.sp)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                    Text(
                                        text = "⚠️ Problem: " + when (selectedLanguage) { Language.ENGLISH -> cb.problemEn; Language.HINDI -> cb.problemHi; Language.KANNADA -> cb.problemKn },
                                        fontSize = 12.sp, color = Color(0xFFE5E5E5)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "✅ Solution: " + when (selectedLanguage) { Language.ENGLISH -> cb.solutionEn; Language.HINDI -> cb.solutionHi; Language.KANNADA -> cb.solutionKn },
                                        fontSize = 12.sp, color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        color = Color(0xFF1C1C1C), shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF404040), RoundedCornerShape(6.dp))
                                    ) {
                                        Text(text = "🔄 ${cb.counterOfferDraft}", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium, modifier = Modifier.padding(8.dp), lineHeight = 16.sp)
                                    }
                                }
                            }
                        }
                    }

                    if (result.deadlines.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = when (selectedLanguage) {
                                Language.ENGLISH -> "⏰ Key Deadlines & Obligations"
                                Language.HINDI -> "⏰ प्रमुख समय सीमाएं और दायित्व"
                                Language.KANNADA -> "⏰ ಪ್ರಮುಖ ಗಡುವುಗಳು ಮತ್ತು ಜವಾಬ್ದಾರಿಗಳು"
                            },
                            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        result.deadlines.forEach { deadline ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = Color(0xFF262626),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.padding(end = 12.dp)
                                        ) {
                                            Text(deadline.timeframe, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Text(
                                            text = when (selectedLanguage) { Language.ENGLISH -> deadline.obligationEn; Language.HINDI -> deadline.obligationHi; Language.KANNADA -> deadline.obligationKn },
                                            fontSize = 12.sp, color = Color(0xFFE2E8F0)
                                        )
                                    }
                                    if (deadline.quoteSnippet.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            color = Color(0xFF0A0A0A), shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(6.dp))
                                        ) {
                                            Text(text = "“Quoted Clause: ${deadline.quoteSnippet}”", fontSize = 11.sp, color = Color(0xFFCCCCCC), modifier = Modifier.padding(8.dp), lineHeight = 16.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (result.financialExposures.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = when (selectedLanguage) {
                                Language.ENGLISH -> "💰 Financial Exposure & Hidden Costs"
                                Language.HINDI -> "💰 वित्तीय जोखिम और छिपी लागतें"
                                Language.KANNADA -> "💰 ಹಣಕಾಸು ಅಪಾಯ ಮತ್ತು ಗುಪ್ತ ವೆಚ್ಚಗಳು"
                            },
                            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        result.financialExposures.forEach { fin ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Text(
                                                text = when (selectedLanguage) { Language.ENGLISH -> fin.titleEn; Language.HINDI -> fin.titleHi; Language.KANNADA -> fin.titleKn },
                                                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = when (selectedLanguage) { Language.ENGLISH -> fin.descriptionEn; Language.HINDI -> fin.descriptionHi; Language.KANNADA -> fin.descriptionKn },
                                                fontSize = 11.sp, color = Color(0xFFA0A0A0)
                                            )
                                        }

                                        Surface(
                                            color = Color(0xFF262626),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = formatCompactAmount(fin.amountOrCost),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    if (fin.quoteSnippet.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            color = Color(0xFF0A0A0A), shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(6.dp))
                                        ) {
                                            Text(text = "“Quoted Clause: ${fin.quoteSnippet}”", fontSize = 11.sp, color = Color(0xFFCCCCCC), modifier = Modifier.padding(8.dp), lineHeight = 16.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (result.ambiguities.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = when (selectedLanguage) {
                                Language.ENGLISH -> "🔍 Ambiguity & Discretion Detector"
                                Language.HINDI -> "🔍 अस्पष्टता और विवेक डिटेक्टर"
                                Language.KANNADA -> "🔍 ಅಸ್ಪಷ್ಟತೆ ಮತ್ತು ವಿವೇಚನೆ ಪತ್ತೆ"
                            },
                            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        result.ambiguities.forEach { amb ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(amb.phrase, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = when (selectedLanguage) { Language.ENGLISH -> amb.explanationEn; Language.HINDI -> amb.explanationHi; Language.KANNADA -> amb.explanationKn },
                                        fontSize = 12.sp, color = Color(0xFFCCCCCC)
                                    )
                                    if (amb.quoteSnippet.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            color = Color(0xFF0A0A0A), shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(6.dp))
                                        ) {
                                            Text(text = "“Quoted Clause: ${amb.quoteSnippet}”", fontSize = 11.sp, color = Color(0xFFCCCCCC), modifier = Modifier.padding(8.dp), lineHeight = 16.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (checklistStates.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = when (selectedLanguage) {
                                Language.ENGLISH -> "📋 Pre-Signing Resolution Checklist"
                                Language.HINDI -> "📋 हस्ताक्षर-पूर्व समाधान चेकलिस्ट"
                                Language.KANNADA -> "📋 ಸಹಿ ಮಾಡುವ ಮುನ್ನ ಪರಿಶೀಲನಾ ಪಟ್ಟಿ"
                            },
                            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                checklistStates.forEachIndexed { index, item ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                checklistStates[index] = item.copy(isResolved = !item.isResolved)
                                            }
                                            .padding(vertical = 6.dp, horizontal = 4.dp)
                                    ) {
                                        Checkbox(
                                            checked = item.isResolved,
                                            onCheckedChange = { checked ->
                                                checklistStates[index] = item.copy(isResolved = checked)
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = Color.White,
                                                checkmarkColor = Color.Black,
                                                uncheckedColor = Color(0xFF666666)
                                            )
                                        )
                                        Text(
                                            text = when (selectedLanguage) {
                                                Language.ENGLISH -> item.taskEn
                                                Language.HINDI -> item.taskHi
                                                Language.KANNADA -> item.taskKn
                                            },
                                            fontSize = 12.sp,
                                            color = if (item.isResolved) Color(0xFF666666) else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = when (selectedLanguage) {
                            Language.ENGLISH -> "📖 Tap-to-Define Legal Dictionary"
                            Language.HINDI -> "📖 टैप-टू-डिफाइन कानूनी शब्दकोश"
                            Language.KANNADA -> "📖 ಟ್ಯಾಪ್ ಮಾಡಿ ಅರ್ಥ ತಿಳಿಯುವ ಶಾಸನ ನಿಘಂಟು"
                        },
                        fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        val dict = listOf(
                            "Indemnity" to "Obligation to compensate for damages or losses incurred by the other party.",
                            "Arbitration" to "Private dispute resolution outside formal government court systems.",
                            "Jurisdiction" to "The specific court system authorized to settle legal claims."
                        )
                        dict.forEach { (term, def) ->
                            Button(
                                onClick = { selectedDictWord = term to def },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF121212)),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF333333))
                            ) {
                                Text(term, fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            contractText = ""
                            analysisResult = null
                            appState = AppState.INPUT
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Text("Scan Another Contract", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }

        // Full Screen Slide-In History Drawer (Monochrome Style)
        AnimatedVisibility(
            visible = showHistoryDrawer,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
            modifier = Modifier.zIndex(10f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable { showHistoryDrawer = false }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.88f)
                        .background(Color(0xFF0F0F0F))
                        .clickable(enabled = false) {}
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Offline History", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                Text("Past scanned contracts & reports", color = Color(0xFFA0A0A0), fontSize = 12.sp)
                            }

                            IconButton(onClick = { showHistoryDrawer = false }) {
                                Text("✕", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (historyItems.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No past contract scans found.", color = Color(0xFF666666), fontSize = 14.sp)
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                historyItems.forEach { item ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                            .clickable {
                                                contractText = item.fullText
                                                showHistoryDrawer = false
                                                appState = AppState.PROCESSING_LOADING
                                            },
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF181818)),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color(0xFF333333))
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = item.title,
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    color = Color(0xFF333333),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = item.riskName,
                                                        color = Color.White,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = item.snippet,
                                                color = Color(0xFFCCCCCC),
                                                fontSize = 12.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Tap to load full past analysis report ➔",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedDictWord != null) {
        AlertDialog(
            onDismissRequest = { selectedDictWord = null },
            title = { Text(selectedDictWord!!.first, color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text(selectedDictWord!!.second, color = Color(0xFFCCCCCC), fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = { selectedDictWord = null }) { Text("Close", color = Color.White) }
            },
            containerColor = Color(0xFF181818)
        )
    }
}

private fun drawWrappedPdfText(
    pageManager: () -> Canvas,
    text: String,
    x: Float,
    startY: Float,
    maxWidth: Float,
    paint: Paint,
    lineHeight: Float = 14f,
    onPageCheck: (Float) -> Unit
): Float {
    var currentY = startY
    val words = text.split(" ")
    var currentLine = StringBuilder()

    for (word in words) {
        val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
        if (paint.measureText(testLine) <= maxWidth) {
            currentLine.append(if (currentLine.isEmpty()) word else " $word")
        } else {
            onPageCheck(lineHeight)
            pageManager().drawText(currentLine.toString(), x, currentY, paint)
            currentY += lineHeight
            currentLine = StringBuilder(word)
        }
    }
    if (currentLine.isNotEmpty()) {
        onPageCheck(lineHeight)
        pageManager().drawText(currentLine.toString(), x, currentY, paint)
        currentY += lineHeight
    }
    return currentY
}

private fun generatePdfReport(context: Context, result: AnalysisResult, text: String, lang: Language) {
    try {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var pageCount = 1
        var currentPage = document.startPage(pageInfo)
        val paint = Paint().apply { isAntiAlias = true }

        fun drawPageBackground(canvas: Canvas, isFirstPage: Boolean) {
            // Crisp White Document Paper Background
            paint.color = android.graphics.Color.WHITE
            canvas.drawRect(0f, 0f, 595f, 842f, paint)

            if (isFirstPage) {
                // Professional Dark Header Banner
                val headerPaint = Paint().apply { color = android.graphics.Color.parseColor("#111827") }
                canvas.drawRect(0f, 0f, 595f, 85f, headerPaint)

                val accentPaint = Paint().apply { color = android.graphics.Color.parseColor("#374151") }
                canvas.drawRect(0f, 82f, 595f, 85f, accentPaint)

                paint.textSize = 20f
                paint.isFakeBoldText = true
                paint.color = android.graphics.Color.WHITE
                canvas.drawText("VerdictEdge Legal Analysis Report", 36f, 48f, paint)

                paint.textSize = 9f
                paint.isFakeBoldText = false
                paint.color = android.graphics.Color.parseColor("#9CA3AF")
                canvas.drawText("Air-gapped Local Legal Intelligence Engine", 36f, 66f, paint)
            } else {
                // Top header bar on subsequent pages
                val linePaint = Paint().apply { color = android.graphics.Color.parseColor("#E5E7EB") }
                canvas.drawLine(36f, 40f, 559f, 40f, linePaint)
                paint.textSize = 8f
                paint.isFakeBoldText = false
                paint.color = android.graphics.Color.parseColor("#6B7280")
                canvas.drawText("VerdictEdge Analysis Report (Continued)", 36f, 32f, paint)
            }

            // Bottom Footer
            val footerLinePaint = Paint().apply { color = android.graphics.Color.parseColor("#E5E7EB") }
            canvas.drawLine(36f, 800f, 559f, 800f, footerLinePaint)

            paint.textSize = 8f
            paint.isFakeBoldText = false
            paint.color = android.graphics.Color.parseColor("#9CA3AF")
            canvas.drawText("CONFIDENTIAL - FOR PERSONAL REFERENCE ONLY", 36f, 815f, paint)
            canvas.drawText("Page $pageCount", 520f, 815f, paint)
        }

        val canvasProvider = { currentPage.canvas }
        drawPageBackground(canvasProvider(), true)

        var yPos = 110f

        fun checkPageOverflow(neededHeight: Float) {
            if (yPos + neededHeight > 780f) {
                document.finishPage(currentPage)
                pageCount++
                currentPage = document.startPage(pageInfo)
                drawPageBackground(canvasProvider(), false)
                yPos = 55f
            }
        }

        // Overall Risk Assessment Card Box
        checkPageOverflow(55f)
        val cardBgPaint = Paint().apply { color = android.graphics.Color.parseColor("#F9FAFB") }
        val cardBorderPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#E5E7EB")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvasProvider().drawRect(36f, yPos, 559f, yPos + 55f, cardBgPaint)
        canvasProvider().drawRect(36f, yPos, 559f, yPos + 55f, cardBorderPaint)

        paint.textSize = 13f
        paint.isFakeBoldText = true
        val riskLabel = when (lang) {
            Language.ENGLISH -> result.riskLevel.labelEn
            Language.HINDI -> result.riskLevel.labelHi
            Language.KANNADA -> result.riskLevel.labelKn
        }
        paint.color = result.riskLevel.color.toArgb()
        canvasProvider().drawText("Overall Assessment: $riskLabel", 48f, yPos + 22f, paint)

        val summaryText = when (lang) {
            Language.ENGLISH -> result.summaryEn
            Language.HINDI -> result.summaryHi
            Language.KANNADA -> result.summaryKn
        }
        paint.textSize = 9.5f
        paint.isFakeBoldText = false
        paint.color = android.graphics.Color.parseColor("#374151")
        drawWrappedPdfText(canvasProvider, summaryText, 48f, yPos + 38f, 498f, paint, 12f) { checkPageOverflow(it) }

        yPos += 70f

        // Statutory Warnings
        if (result.statutoryVoidabilities.isNotEmpty()) {
            checkPageOverflow(30f)
            paint.isFakeBoldText = true
            paint.textSize = 11f
            paint.color = android.graphics.Color.parseColor("#DC2626")
            canvasProvider().drawText("Statutory Warnings (Indian Contract Law):", 36f, yPos, paint)
            yPos += 18f
            paint.textSize = 9.5f

            result.statutoryVoidabilities.forEach { s ->
                checkPageOverflow(25f)
                val title = when (lang) { Language.ENGLISH -> s.titleEn; Language.HINDI -> s.titleHi; Language.KANNADA -> s.titleKn }
                val reason = when (lang) { Language.ENGLISH -> s.legalReasonEn; Language.HINDI -> s.legalReasonHi; Language.KANNADA -> s.legalReasonKn }

                paint.isFakeBoldText = true
                paint.color = android.graphics.Color.parseColor("#1F2937")
                canvasProvider().drawText("• ${s.actSection}: $title", 42f, yPos, paint)
                yPos += 14f
                paint.isFakeBoldText = false
                paint.color = android.graphics.Color.parseColor("#4B5563")
                yPos = drawWrappedPdfText(canvasProvider, reason, 52f, yPos, 490f, paint, 12f) { checkPageOverflow(it) }

                if (s.quoteSnippet.isNotBlank()) {
                    yPos = drawWrappedPdfText(canvasProvider, "Quoted: \"${s.quoteSnippet}\"", 52f, yPos, 490f, paint, 12f) { checkPageOverflow(it) }
                }
                yPos += 6f
            }
            yPos += 10f
        }

        // Vulnerabilities & Counter-Offers
        if (result.clauseBreakdowns.isNotEmpty()) {
            checkPageOverflow(30f)
            paint.isFakeBoldText = true
            paint.textSize = 11f
            paint.color = android.graphics.Color.parseColor("#1D4ED8")
            canvasProvider().drawText("Vulnerabilities & Counter-Offer Guidance:", 36f, yPos, paint)
            yPos += 18f
            paint.textSize = 9.5f

            result.clauseBreakdowns.forEachIndexed { index, cb ->
                checkPageOverflow(45f)
                val probText = when (lang) { Language.ENGLISH -> cb.problemEn; Language.HINDI -> cb.problemHi; Language.KANNADA -> cb.problemKn }
                val solText = when (lang) { Language.ENGLISH -> cb.solutionEn; Language.HINDI -> cb.solutionHi; Language.KANNADA -> cb.solutionKn }

                paint.isFakeBoldText = true
                paint.color = android.graphics.Color.parseColor("#111827")
                yPos = drawWrappedPdfText(canvasProvider, "${index + 1}. Problem: $probText", 42f, yPos, 500f, paint, 13f) { checkPageOverflow(it) }

                paint.isFakeBoldText = false
                paint.color = android.graphics.Color.parseColor("#374151")
                if (cb.originalSnippet.isNotBlank()) {
                    yPos = drawWrappedPdfText(canvasProvider, "   Quoted Clause: \"${cb.originalSnippet}\"", 50f, yPos, 490f, paint, 12f) { checkPageOverflow(it) }
                }
                yPos = drawWrappedPdfText(canvasProvider, "   Solution: $solText", 50f, yPos, 490f, paint, 12f) { checkPageOverflow(it) }

                paint.color = android.graphics.Color.parseColor("#B45309")
                yPos = drawWrappedPdfText(canvasProvider, "   Proposed Counter-Clause: ${cb.counterOfferDraft}", 50f, yPos, 490f, paint, 12f) { checkPageOverflow(it) }
                yPos += 8f
            }
            yPos += 10f
        }

        // Financial Exposure
        if (result.financialExposures.isNotEmpty()) {
            checkPageOverflow(30f)
            paint.isFakeBoldText = true
            paint.textSize = 11f
            paint.color = android.graphics.Color.parseColor("#D97706")
            canvasProvider().drawText("Financial Exposure & Costs:", 36f, yPos, paint)
            yPos += 18f
            paint.isFakeBoldText = false
            paint.textSize = 9.5f
            paint.color = android.graphics.Color.parseColor("#111827")

            result.financialExposures.forEach { f ->
                checkPageOverflow(20f)
                val fTitle = when (lang) { Language.ENGLISH -> f.titleEn; Language.HINDI -> f.titleHi; Language.KANNADA -> f.titleKn }
                canvasProvider().drawText("• $fTitle [${f.amountOrCost}]", 42f, yPos, paint)
                yPos += 14f
                if (f.quoteSnippet.isNotBlank()) {
                    yPos = drawWrappedPdfText(canvasProvider, "   Quoted: \"${f.quoteSnippet}\"", 50f, yPos, 490f, paint, 12f) { checkPageOverflow(it) }
                }
            }
            yPos += 10f
        }

        // Key Deadlines
        if (result.deadlines.isNotEmpty()) {
            checkPageOverflow(30f)
            paint.isFakeBoldText = true
            paint.textSize = 11f
            paint.color = android.graphics.Color.parseColor("#059669")
            canvasProvider().drawText("Key Deadlines & Obligations:", 36f, yPos, paint)
            yPos += 18f
            paint.isFakeBoldText = false
            paint.textSize = 9.5f
            paint.color = android.graphics.Color.parseColor("#111827")

            result.deadlines.forEach { d ->
                checkPageOverflow(20f)
                val dOb = when (lang) { Language.ENGLISH -> d.obligationEn; Language.HINDI -> d.obligationHi; Language.KANNADA -> d.obligationKn }
                canvasProvider().drawText("• [${d.timeframe}] $dOb", 42f, yPos, paint)
                yPos += 14f
                if (d.quoteSnippet.isNotBlank()) {
                    yPos = drawWrappedPdfText(canvasProvider, "   Quoted: \"${d.quoteSnippet}\"", 50f, yPos, 490f, paint, 12f) { checkPageOverflow(it) }
                }
            }
            yPos += 10f
        }

        // Checklist
        if (result.preSigningChecklist.isNotEmpty()) {
            checkPageOverflow(30f)
            paint.isFakeBoldText = true
            paint.textSize = 11f
            paint.color = android.graphics.Color.parseColor("#4F46E5")
            canvasProvider().drawText("Pre-Signing Resolution Checklist:", 36f, yPos, paint)
            yPos += 18f
            paint.isFakeBoldText = false
            paint.textSize = 9.5f
            paint.color = android.graphics.Color.parseColor("#111827")

            result.preSigningChecklist.forEach { ch ->
                checkPageOverflow(20f)
                val task = when (lang) { Language.ENGLISH -> ch.taskEn; Language.HINDI -> ch.taskHi; Language.KANNADA -> ch.taskKn }
                canvasProvider().drawText("[  ] $task", 42f, yPos, paint)
                yPos += 14f
            }
        }

        document.finishPage(currentPage)

        val file = File(context.cacheDir, "VerdictEdge_Risk_Report.pdf")
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(intent, "Open Risk PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "PDF saved to cache. Install PDF viewer to open.", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to generate PDF report", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun OfficialTheme(content: @Composable () -> Unit) {
    val darkColorScheme = darkColorScheme(
        primary = Color.White,
        onPrimary = Color.Black,
        background = Color(0xFF000000),
        surface = Color(0xFF121212),
        onSurface = Color.White,
        onSurfaceVariant = Color(0xFFA0A0A0),
        outline = Color(0xFF2A2A2A)
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color(0xFF000000).toArgb()
            window.navigationBarColor = Color(0xFF000000).toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(colorScheme = darkColorScheme, content = content)
}