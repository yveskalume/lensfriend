package com.yveskalume.lensfriend.ui.screens.camera

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel

class CameraViewModel : ViewModel() {

    val images: SnapshotStateList<Bitmap> = mutableStateListOf()

    fun addImage(bitmap: Bitmap) {
        images.add(bitmap)
    }

}