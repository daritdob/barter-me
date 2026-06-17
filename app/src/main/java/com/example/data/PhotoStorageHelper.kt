package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PhotoStorageHelper {

    // Generate a secure, unique file in the app's internal files directory for permanent offline storage
    fun createOfflineImageFile(context: Context): File {
        val storageDir = context.filesDir
        return File(storageDir, "trade_item_${System.currentTimeMillis()}.jpg")
    }

    // Generate a temporary cache file for the Camera TakePicture API
    fun createTempImageFile(context: Context): File {
        val cacheDir = context.cacheDir
        return File(cacheDir, "temp_camera_${System.currentTimeMillis()}.jpg")
    }

    // Get content URI using the FileProvider defined in AndroidManifest.xml
    fun getUriForFile(context: Context, file: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    private fun toPersistentUriString(context: Context, file: File): String =
        getUriForFile(context, file).toString()

    // Copy temporary camera file to permanent files directory for persistent offline retrieval
    fun saveTempFileToOfflineStorage(context: Context, tempFile: File): String {
        val destFile = createOfflineImageFile(context)
        try {
            tempFile.inputStream().use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            return toPersistentUriString(context, destFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return toPersistentUriString(context, tempFile)
    }

    // Save a simulated photo to local private files directory
    suspend fun saveSimulatedItemPhoto(context: Context, itemName: String, category: String): String {
        return withContext(Dispatchers.IO) {
            val destFile = createOfflineImageFile(context)
            var success = false

            // Preset random high-quality items for visual flair
            val simulatedUrls = listOf(
                "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=400", // Red Sneaker
                "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400", // Headset
                "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=400", // White Watch
                "https://images.unsplash.com/photo-1572635196237-14b3f281503f?w=400"  // Sunglasses
            )
            val selectedUrl = simulatedUrls.random()

            try {
                URL(selectedUrl).openStream().use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                success = true
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (!success) {
                // Generate a programmatic gradient Bitmap with text so it works offline instantly!
                try {
                    val width = 450
                    val height = 450
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    
                    // Gradient paint
                    val paint = android.graphics.Paint()
                    val shader = android.graphics.LinearGradient(
                        0f, 0f, width.toFloat(), height.toFloat(),
                        android.graphics.Color.parseColor("#4F46E5"), // Purple
                        android.graphics.Color.parseColor("#06B6D4"), // Cyan
                        android.graphics.Shader.TileMode.CLAMP
                    )
                    paint.shader = shader
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

                    // Text paint
                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 28f
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }

                    val label = if (itemName.length > 20) itemName.take(17) + "..." else itemName
                    canvas.drawText(label, width / 2f, height / 2f - 10f, textPaint)
                    
                    textPaint.textSize = 20f
                    textPaint.color = android.graphics.Color.parseColor("#E0E7FF")
                    canvas.drawText("Category: $category", width / 2f, height / 2f + 30f, textPaint)

                    textPaint.textSize = 14f
                    textPaint.color = android.graphics.Color.parseColor("#93C5FD")
                    canvas.drawText("[OFFLINE LOCAL STORAGE]", width / 2f, height / 2f + 70f, textPaint)

                    FileOutputStream(destFile).use { output ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
                    }
                    success = true
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
            }

            toPersistentUriString(context, destFile)
        }
    }
}
