package com.eatwhat.ui.viewmodel

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eatwhat.data.model.GalleryImage
import com.eatwhat.data.repository.GalleryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleryViewModel(private val repository: GalleryRepository) : ViewModel() {

    private val _images = mutableStateOf<List<GalleryImage>>(emptyList())
    val images: State<List<GalleryImage>> = _images

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _images.value = repository.getGalleryImages()
            _isLoading.value = false
        }
    }

    fun deleteImage(id: String) {
        repository.removeFromGallery(id)
        refresh()
    }

    fun clearAll() {
        repository.clearGallery()
        refresh()
    }

    fun saveBitmap(context: Context, bitmap: Bitmap, filename: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val saved = saveBitmapToGallery(context, bitmap, filename)
            withContext(Dispatchers.Main) {
                onResult(saved)
            }
        }
    }

    private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, filename: String): Boolean {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$filename.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/食见")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        return if (imageUri != null) {
            resolver.openOutputStream(imageUri).use { outputStream ->
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(imageUri, contentValues, null, null)
                    }
                    true
                } else false
            }
        } else false
    }
}
