package com.example.clausehawk

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

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

private fun createTempImageUri(context: Context): Uri {
    val tempFile = File.createTempFile("scan_doc_", ".jpg", context.cacheDir).apply {
        createNewFile()
        deleteOnExit()
    }
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MonochromeTheme {
                ClauseHawkApp()
            }
        }
    }
}

@Composable
fun ClauseHawkApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val ocrManager = remember { OcrManager(context) }
    val contractEngine = remember { ContractEngine() }

    var appState by remember { mutableStateOf(AppState.INIT_LOADING) }
    var contractText by remember { mutableStateOf("") }
    var analysisResult by remember { mutableStateOf<AnalysisResult?>(null) }
    var isOcrExtracting by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf(Language.ENGLISH) }

    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoUri != null) {
            isOcrExtracting = true
            ocrManager.processImageFromUri(
                uri = tempPhotoUri!!,
                onSuccess = { extractedText ->
                    isOcrExtracting = false
                    if (extractedText.isNotBlank()) {
                        contractText = extractedText
                    } else {
                        Toast.makeText(context, "No text detected in photo", Toast.LENGTH_SHORT).show()
                    }
                },
                onError = {
                    isOcrExtracting = false
                    Toast.makeText(context, "OCR failed to read photo", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    val launchFullResCamera = {
        try {
            val uri = createTempImageUri(context)
            tempPhotoUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to launch camera app", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchFullResCamera()
        } else {
            Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isOcrExtracting = true
            ocrManager.processImageFromUri(
                uri = uri,
                onSuccess = { extractedText ->
                    isOcrExtracting = false
                    if (extractedText.isNotBlank()) {
                        contractText = extractedText
                    } else {
                        Toast.makeText(context, "No text detected in document", Toast.LENGTH_SHORT).show()
                    }
                },
                onError = {
                    isOcrExtracting = false
                    Toast.makeText(context, "OCR failed to read image", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    if (appState == AppState.INIT_LOADING) {
        LaunchedEffect(Unit) {
            delay(1800L)
            appState = AppState.INPUT
        }

        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(modifier = Modifier.size(36.dp), color = Color.White, strokeWidth = 2.5.dp)
                Spacer(modifier = Modifier.height(28.dp))
                Text(text = "Gemma 2B Initialized", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Air-gapped local environment ready", fontSize = 13.sp, color = Color(0xFF888888))
            }
        }
    }

    if (appState == AppState.INPUT) {
        Surface(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(Color(0xFF141414))
                            .border(1.dp, Color(0xFF262626), RoundedCornerShape(50.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isOcrExtracting) "Scanning (En / Hi / Kn)..." else "Gemma 2B Engine Active",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFCCCCCC)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = "VerdictEdge", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = "Enter text or scan English, Hindi, Kannada legal document", fontSize = 13.sp, color = Color(0xFF888888))

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = contractText,
                        onValueChange = { contractText = it },
                        placeholder = {
                            Text(
                                text = if (isOcrExtracting) "Extracting OCR text..." else "Paste contract text (English / हिंदी / ಕನ್ನಡ) here...",
                                color = Color(0xFF555555),
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF101010),
                            unfocusedContainerColor = Color(0xFF101010),
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color(0xFF262626),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                if (hasPermission) launchFullResCamera() else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF181818), contentColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFF262626))
                        ) {
                            Text(text = "📷 Camera", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }

                        Button(
                            onClick = {
                                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF181818), contentColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFF262626))
                        ) {
                            Text(text = "🖼️ Gallery", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Button(
                    onClick = {
                        if (contractText.isNotBlank()) {
                            appState = AppState.PROCESSING_LOADING
                        }
                    },
                    enabled = contractText.isNotBlank() && !isOcrExtracting,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                        disabledContainerColor = Color(0xFF222222),
                        disabledContentColor = Color(0xFF555555)
                    )
                ) {
                    Text(text = "Analyze Contract", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (appState == AppState.PROCESSING_LOADING) {
        LaunchedEffect(Unit) {
            coroutineScope.launch {
                analysisResult = contractEngine.analyzeContract(contractText)
                appState = AppState.RESULTS
            }
        }

        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(modifier = Modifier.size(40.dp), color = Color.White, strokeWidth = 3.dp)
                Spacer(modifier = Modifier.height(28.dp))
                Text(text = "Processing Legal Text...", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Evaluating clause risk factors with Gemma 2B", fontSize = 13.sp, color = Color(0xFF888888))
            }
        }
    }

    if (appState == AppState.RESULTS && analysisResult != null) {
        val result = analysisResult!!
        val scrollState = rememberScrollState()

        Surface(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Analysis Report", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = "Multilingual Local AI Evaluation", fontSize = 13.sp, color = Color(0xFF888888))

                    Spacer(modifier = Modifier.height(16.dp))

                    // Language Selector Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF141414))
                            .border(1.dp, Color(0xFF262626), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
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
                                    text = when (lang) {
                                        Language.ENGLISH -> "English"
                                        Language.HINDI -> "हिंदी"
                                        Language.KANNADA -> "ಕನ್ನಡ"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else Color(0xFF888888)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Invalid Text Alert
                    if (result.isInvalid) {
                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFEF4444), RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1010))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "⚠️ INVALID INPUT",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = when (selectedLanguage) {
                                        Language.ENGLISH -> result.summaryEn
                                        Language.HINDI -> result.summaryHi
                                        Language.KANNADA -> result.summaryKn
                                    },
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        // Risk Badge Card
                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF262626), RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF101010))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "RISK FACTOR",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF888888),
                                    letterSpacing = 1.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(result.riskLevel.color))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = when (selectedLanguage) {
                                            Language.ENGLISH -> result.riskLevel.labelEn
                                            Language.HINDI -> result.riskLevel.labelHi
                                            Language.KANNADA -> result.riskLevel.labelKn
                                        },
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = when (selectedLanguage) {
                                        Language.ENGLISH -> result.riskLevel.descEn
                                        Language.HINDI -> result.riskLevel.descHi
                                        Language.KANNADA -> result.riskLevel.descKn
                                    },
                                    fontSize = 13.sp,
                                    color = Color(0xFFCCCCCC),
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Key Red Flags
                        Text(
                            text = when (selectedLanguage) {
                                Language.ENGLISH -> "Key Points to Address"
                                Language.HINDI -> "मुख्य विचारणीय बिंदु"
                                Language.KANNADA -> "ಗಮನಿಸಬೇಕಾದ ಪ್ರಮುಖ ಅಂಶಗಳು"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF262626), RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF101010))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                result.redFlags.forEachIndexed { index, flag ->
                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Row(verticalAlignment = Alignment.Top) {
                                            Text(text = "•", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = when (selectedLanguage) {
                                                        Language.ENGLISH -> flag.titleEn
                                                        Language.HINDI -> flag.titleHi
                                                        Language.KANNADA -> flag.titleKn
                                                    },
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = when (selectedLanguage) {
                                                        Language.ENGLISH -> flag.descEn
                                                        Language.HINDI -> flag.descHi
                                                        Language.KANNADA -> flag.descKn
                                                    },
                                                    fontSize = 13.sp,
                                                    color = Color(0xFFDDDDDD),
                                                    lineHeight = 18.sp
                                                )
                                            }
                                        }
                                    }
                                    if (index < result.redFlags.size - 1) {
                                        HorizontalDivider(color = Color(0xFF1E1E1E), modifier = Modifier.padding(vertical = 8.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Summary Block
                        Text(
                            text = when (selectedLanguage) {
                                Language.ENGLISH -> "Summary"
                                Language.HINDI -> "सारांश"
                                Language.KANNADA -> "ಸಾರಾಂಶ"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF262626), RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF101010))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = when (selectedLanguage) {
                                        Language.ENGLISH -> result.summaryEn
                                        Language.HINDI -> result.summaryHi
                                        Language.KANNADA -> result.summaryKn
                                    },
                                    fontSize = 13.sp,
                                    color = Color(0xFFCCCCCC),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                    Button(
                        onClick = {
                            contractText = ""
                            analysisResult = null
                            appState = AppState.INPUT
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Text(text = "Check More Text", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MonochromeTheme(content: @Composable () -> Unit) {
    val darkColorScheme = darkColorScheme(
        primary = Color.White,
        onPrimary = Color.Black,
        background = Color.Black,
        surface = Color(0xFF101010),
        onSurface = Color.White,
        onSurfaceVariant = Color(0xFF888888),
        outline = Color(0xFF262626)
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Black.toArgb()
            window.navigationBarColor = Color.Black.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme,
        content = content
    )
}