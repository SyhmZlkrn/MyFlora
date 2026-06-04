package com.example.flora.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object BitmapStorage {
    private const val DIR = "plant_images"

    fun save(context: Context, bitmap: Bitmap): String? {
        return try {
            val dir = File(context.filesDir, DIR).apply { if (!exists()) mkdirs() }
            val file = File(dir, "plant_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun saveFromUri(context: Context, uri: Uri): String? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = input.use { BitmapFactory.decodeStream(it) } ?: return null
            save(context, bitmap)
        } catch (e: Exception) {
            null
        }
    }

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        try { File(path).takeIf { it.exists() }?.delete() } catch (_: Exception) {}
    }
}
