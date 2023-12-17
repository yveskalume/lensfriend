package com.yveskalume.lensfriend.ui.screens.camera

import android.graphics.Bitmap
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.yveskalume.lensfriend.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class CameraViewModel : ViewModel() {

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-pro-vision",
            apiKey = BuildConfig.GEMINI_API_KEY,
        )
    }

    val isLoading: MutableState<Boolean> = mutableStateOf(false)
    val error: MutableState<String?> = mutableStateOf(null)
    val result: MutableState<String> = mutableStateOf("")

    val images: SnapshotStateList<Bitmap> = mutableStateListOf()

    fun addImage(bitmap: Bitmap) {
        images.add(bitmap)
    }

    fun sendPrompt(prompt: String) {
        viewModelScope.launch(Dispatchers.IO) {
            isLoading.value = true
            val inputContent = content {
                images.forEach { imageBitmap ->
                    image(imageBitmap)
                }
                text(prompt)
            }

            generativeModel.generateContentStream(inputContent)
                .catch {
                    error.value = it.localizedMessage
                }
                .collect { chunk ->
                    result.value += chunk.text
                }
            isLoading.value = false
        }
    }

    fun reset() {
        viewModelScope.launch {
            result.value = ""
            error.value = null
            images.clear()
        }
    }

    fun removeImage(image: Bitmap) {
        images.remove(image)
    }

}