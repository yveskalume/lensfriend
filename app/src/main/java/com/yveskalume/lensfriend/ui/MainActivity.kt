package com.yveskalume.lensfriend.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.yveskalume.lensfriend.ui.screens.camera.CameraScreen
import com.yveskalume.lensfriend.ui.theme.LensfriendTheme

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LensfriendTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CameraScreen()
                }
            }
        }
        askCameraPermission()
    }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun askCameraPermission() {
        val launcher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted.not()) {
                Toast.makeText(
                    this,
                    "Camera permission is needed to take a picture",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        launcher.launch(Manifest.permission.CAMERA)
    }
}



