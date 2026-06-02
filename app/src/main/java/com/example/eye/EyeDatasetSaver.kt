package com.example.eye

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import java.io.File
import java.io.FileOutputStream

class EyeDatasetSaver(
    private val context: Context
) {

    private fun getBaseDir(): File {
        val dir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "dataset"
        )
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun save(bitmap: Bitmap?, label: String) {
        if (bitmap == null) return

        val baseDir = getBaseDir()
        val labelDir = File(baseDir, label)

        if (!labelDir.exists()) labelDir.mkdirs()

        val fileName = "${label}_${System.currentTimeMillis()}.png"
        val file = File(labelDir, fileName)

        val fos = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        fos.flush()
        fos.close()
    }
}