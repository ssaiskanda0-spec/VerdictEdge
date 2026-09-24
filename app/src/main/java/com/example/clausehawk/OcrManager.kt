package com.example.clausehawk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class OcrManager(private val context: Context) {

    private val recognizer = TextRecognition.getClient(
        DevanagariTextRecognizerOptions.Builder().build()
    )

    fun processImageFromUri(
        uri: Uri,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val mimeType = context.contentResolver.getType(uri)
            if (mimeType == "application/pdf" || uri.toString().endsWith(".pdf", ignoreCase = true)) {
                processPdfUri(uri, onSuccess, onError)
            } else {
                val image = InputImage.fromFilePath(context, uri)
                recognizer.process(image)
                    .addOnSuccessListener { visionText -> onSuccess(visionText.text) }
                    .addOnFailureListener { e -> onError(e) }
            }
        } catch (e: Exception) {
            onError(e)
        }
    }

    suspend fun processMultipleUris(
        uris: List<Uri>,
        onProgress: (Int, Int) -> Unit,
        onComplete: (String) -> Unit,
        onError: (Exception) -> Unit
    ) = withContext(Dispatchers.IO) {
        val limitedUris = uris.take(10)
        val fullExtractedText = StringBuilder()

        for ((index, uri) in limitedUris.withIndex()) {
            onProgress(index + 1, limitedUris.size)
            try {
                val text = extractTextSynchronous(uri)
                if (text.isNotBlank()) {
                    fullExtractedText.append("--- [Document ${index + 1}] ---\n")
                    fullExtractedText.append(text).append("\n\n")
                }
            } catch (e: Exception) {
                // Continue with remaining files on single file error
            }
        }

        withContext(Dispatchers.Main) {
            if (fullExtractedText.isNotBlank()) {
                onComplete(fullExtractedText.toString().trim())
            } else {
                onError(Exception("Failed to extract text from selected documents."))
            }
        }
    }

    private suspend fun extractTextSynchronous(uri: Uri): String = withContext(Dispatchers.IO) {
        val mimeType = context.contentResolver.getType(uri)
        if (mimeType == "application/pdf" || uri.toString().endsWith(".pdf", ignoreCase = true)) {
            return@withContext extractPdfTextSync(uri)
        } else {
            return@withContext kotlin.coroutines.suspendCoroutine { continuation ->
                try {
                    val image = InputImage.fromFilePath(context, uri)
                    recognizer.process(image)
                        .addOnSuccessListener { continuation.resumeWith(Result.success(it.text)) }
                        .addOnFailureListener { continuation.resumeWith(Result.failure(it)) }
                } catch (e: Exception) {
                    continuation.resumeWith(Result.failure(e))
                }
            }
        }
    }

    private fun processPdfUri(uri: Uri, onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
        try {
            val text = extractPdfTextSync(uri)
            if (text.isNotBlank()) onSuccess(text) else onError(Exception("Empty PDF text"))
        } catch (e: Exception) {
            onError(e)
        }
    }

    private fun extractPdfTextSync(uri: Uri): String {
        val extracted = StringBuilder()
        val tempFile = File.createTempFile("pdf_proc_", ".pdf", context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output -> input.copyTo(output) }
        }

        val fileDescriptor = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fileDescriptor)
        val pageCount = minOf(renderer.pageCount, 15) // Max 15 pages per PDF for mobile speed

        for (i in 0 until pageCount) {
            val page = renderer.openPage(i)
            val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            val image = InputImage.fromBitmap(bitmap, 0)
            val result = com.google.android.gms.tasks.Tasks.await(recognizer.process(image))
            extracted.append(result.text).append("\n")
            bitmap.recycle()
        }

        renderer.close()
        fileDescriptor.close()
        tempFile.delete()
        return extracted.toString().trim()
    }

    fun processImageFromBitmap(
        bitmap: Bitmap,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText -> onSuccess(visionText.text) }
                .addOnFailureListener { e -> onError(e) }
        } catch (e: Exception) {
            onError(e)
        }
    }
}