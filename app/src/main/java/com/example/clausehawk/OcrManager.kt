package com.example.clausehawk

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class OcrManager(private val context: Context) {

    private val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val devanagariRecognizer = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())

    fun processImageFromUri(
        uri: Uri,
        language: AppLanguage = AppLanguage.ENGLISH,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            runTextRecognition(image, language, onSuccess, onFailure)
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    fun processImageFromBitmap(
        bitmap: Bitmap,
        language: AppLanguage = AppLanguage.ENGLISH,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            runTextRecognition(image, language, onSuccess, onFailure)
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    private fun runTextRecognition(
        image: InputImage,
        language: AppLanguage,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val recognizer = when (language) {
            AppLanguage.HINDI -> devanagariRecognizer
            else -> latinRecognizer
        }

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                onSuccess(visionText.text)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }
}