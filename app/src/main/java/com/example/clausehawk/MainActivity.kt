package com.example.clausehawk

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

enum class AppState {
    INPUT,
    ANALYZING,
    RESULTS
}

class MainActivity : ComponentActivity() {
    private val contractEngine = ContractEngine()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color.Black,
                    surface = Color(0xFF101010)
                )
            ) {
                ClauseHawkApp(contractEngine = contractEngine)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClauseHawkApp(contractEngine: ContractEngine) {
    val context = LocalContext.current
    val ocrManager = remember { OcrManager(context) }

    var appState by remember { mutableStateOf(AppState.INPUT) }
    var contractText by remember { mutableStateOf("") }
    var analysisResult by remember { mutableStateOf<AnalysisResult?>(null) }
    var selectedLanguage by remember { mutableStateOf(AppLanguage.ENGLISH) }
    var isOcrLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isOcrLoading = true
            ocrManager.processImageFromUri(
                uri = it,
                language = selectedLanguage,
                onSuccess = { extractedText ->
                    contractText = extractedText
                    isOcrLoading = false
                },
                onFailure = {
                    isOcrLoading = false
                }
            )
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            isOcrLoading = true
            ocrManager.processImageFromBitmap(
                bitmap = it,
                language = selectedLanguage,
                onSuccess = { extractedText ->
                    contractText = extractedText
                    isOcrLoading = false
                },
                onFailure = {
                    isOcrLoading = false
                }
            )
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = Color.Black
    ) {
        when (appState) {
            AppState.INPUT -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "ClauseHawk",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "Offline AI Legal Contract Risk Analyzer",
                            fontSize = 14.sp,
                            color = Color(0xFF888888)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Contract Text",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFCCCCCC)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { cameraLauncher.launch(null) },
                                    enabled = !isOcrLoading,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFF333333)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White,
                                        disabledContentColor = Color(0xFF666666)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    if (isOcrLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("📷 Camera", fontSize = 12.sp)
                                    }
                                }

                                OutlinedButton(
                                    onClick = { imagePickerLauncher.launch("image/*") },
                                    enabled = !isOcrLoading,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFF333333)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White,
                                        disabledContentColor = Color(0xFF666666)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("🖼️ Gallery", fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = contractText,
                            onValueChange = { contractText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp),
                            placeholder = {
                                Text(
                                    text = "Paste full contract clauses, terms of service, or capture an image above...",
                                    color = Color(0xFF555555),
                                    fontSize = 14.sp
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF101010),
                                unfocusedContainerColor = Color(0xFF101010),
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color(0xFF262626),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }

                    Button(
                        onClick = {
                            if (contractText.isNotBlank()) {
                                appState = AppState.ANALYZING
                                scope.launch {
                                    val result = contractEngine.analyzeContract(contractText)
                                    analysisResult = result
                                    appState = AppState.RESULTS
                                }
                            }
                        },
                        enabled = contractText.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFF222222),
                            disabledContentColor = Color(0xFF555555)
                        )
                    ) {
                        Text(
                            text = "Analyze Contract Risk",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            AppState.ANALYZING -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Evaluating Agreement Risk...",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Scanning for penalties & unilateral clauses",
                            fontSize = 13.sp,
                            color = Color(0xFF888888)
                        )
                    }
                }
            }

            AppState.RESULTS -> {
                val result = analysisResult
                if (result != null) {
                    if (result.isInvalid) {
                        // UNRECOGNIZED / INVALID DOCUMENT VIEW
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(20.dp))

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF262626), RoundedCornerShape(20.dp)),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF101010))
                            ) {
                                Column(
                                    modifier = Modifier.padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "⚠️",
                                        fontSize = 44.sp
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "Invalid Document",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = result.getSummary(selectedLanguage),
                                        fontSize = 15.sp,
                                        color = Color(0xFFCCCCCC),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 22.sp
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    contractText = ""
                                    analysisResult = null
                                    appState = AppState.INPUT
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                )
                            ) {
                                Text(
                                    text = when (selectedLanguage) {
                                        AppLanguage.ENGLISH -> "Try Again"
                                        AppLanguage.HINDI -> "पुनः प्रयास करें"
                                        AppLanguage.KANNADA -> "ಮತ್ತೆ ಪ್ರಯತ್ನಿಸಿ"
                                    },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        // VALID CONTRACT REPORT VIEW
                        val scrollState = rememberScrollState()

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                                .verticalScroll(scrollState),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Analysis Report",
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            letterSpacing = (-0.5).sp
                                        )
                                        Text(
                                            text = "Local offline risk evaluation",
                                            fontSize = 12.sp,
                                            color = Color(0xFF888888)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AppLanguage.values().forEach { language ->
                                        FilterChip(
                                            selected = selectedLanguage == language,
                                            onClick = { selectedLanguage = language },
                                            label = {
                                                Text(
                                                    text = language.label,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (selectedLanguage == language) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color.White,
                                                selectedLabelColor = Color.Black,
                                                containerColor = Color(0xFF141414),
                                                labelColor = Color(0xFF888888)
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                borderColor = Color(0xFF262626),
                                                selectedBorderColor = Color.White,
                                                enabled = true,
                                                selected = selectedLanguage == language
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color(0xFF262626), RoundedCornerShape(16.dp)),
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
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(result.riskLevel.color)
                                            )

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Text(
                                                text = result.riskLevel.getLabel(selectedLanguage),
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = result.riskLevel.getDescription(selectedLanguage),
                                            fontSize = 13.sp,
                                            color = Color(0xFFCCCCCC),
                                            lineHeight = 18.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    text = when (selectedLanguage) {
                                        AppLanguage.ENGLISH -> "Key Points to Address"
                                        AppLanguage.HINDI -> "मुख्य जोखिम बिंदु"
                                        AppLanguage.KANNADA -> "ಮುಖ್ಯ ಅಪಾಯದಂಶಗಳು"
                                    },
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color(0xFF262626), RoundedCornerShape(16.dp)),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF101010))
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        result.redFlags.forEachIndexed { index, flag ->
                                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                                Row(verticalAlignment = Alignment.Top) {
                                                    Text(
                                                        text = "•",
                                                        color = Color.White,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column {
                                                        Text(
                                                            text = flag.getTitle(selectedLanguage),
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = flag.getDesc(selectedLanguage),
                                                            fontSize = 13.sp,
                                                            color = Color(0xFFDDDDDD),
                                                            lineHeight = 18.sp
                                                        )
                                                    }
                                                }
                                            }
                                            if (index < result.redFlags.size - 1) {
                                                HorizontalDivider(
                                                    color = Color(0xFF1E1E1E),
                                                    modifier = Modifier.padding(vertical = 8.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    text = when (selectedLanguage) {
                                        AppLanguage.ENGLISH -> "Summary"
                                        AppLanguage.HINDI -> "सारांश"
                                        AppLanguage.KANNADA -> "ಸಾರಾಂಶ"
                                    },
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color(0xFF262626), RoundedCornerShape(16.dp)),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF101010))
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Text(
                                            text = result.getSummary(selectedLanguage),
                                            fontSize = 13.sp,
                                            color = Color(0xFFCCCCCC),
                                            fontFamily = FontFamily.Default,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp)
                            ) {
                                Button(
                                    onClick = {
                                        contractText = ""
                                        analysisResult = null
                                        appState = AppState.INPUT
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Text(
                                        text = when (selectedLanguage) {
                                            AppLanguage.ENGLISH -> "Check More Text"
                                            AppLanguage.HINDI -> "दूसरा अनुबंध जांचें"
                                            AppLanguage.KANNADA -> "ಮತ್ತೊಂದು ಒಪ್ಪಂದ ಪರಿಶೀಲಿಸಿ"
                                        },
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
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