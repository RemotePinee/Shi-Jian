package com.eatwhat.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.content.ClipData
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ShareUtil {
    fun shareBitmap(context: Context, bitmap: Bitmap, recipeName: String) {
        try {
            val cachePath = File(context.cacheDir, "shares")
            if (!cachePath.exists()) cachePath.mkdirs()
            
            val file = File(cachePath, "recipe_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            
            val contentUri = FileProvider.getUriForFile(
                context, 
                "${context.packageName}.file_provider", 
                file
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                setDataAndType(contentUri, "image/jpeg")
                clipData = ClipData.newRawUri("Recipe Image", contentUri)
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TITLE, "分享自『食见』")
                putExtra(Intent.EXTRA_TEXT, "我在『食见』发现了这道超赞的【$recipeName】，快来看看！")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "分享这道美味"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
