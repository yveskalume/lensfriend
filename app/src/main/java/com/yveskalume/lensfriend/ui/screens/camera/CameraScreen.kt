package com.yveskalume.lensfriend.ui.screens.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yveskalume.lensfriend.util.VoiceRecognitionContract
import com.yveskalume.lensfriend.util.getCameraProvider
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CameraScreen(viewModel: CameraViewModel = viewModel()) {

    val context: Context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    val imageCapture: ImageCapture = remember {
        ImageCapture.Builder().build()
    }

    val cameraSelector = remember(lensFacing) {
        CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
    }

    val preview = remember {
        Preview.Builder().build()
    }

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            setBackgroundColor(android.graphics.Color.BLACK)
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_START
        }
    }

    LaunchedEffect(lensFacing) {
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture
        )
        preview.setSurfaceProvider(previewView.surfaceProvider)
    }


    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true
    )

    val sheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = sheetState
    )

    val coroutineScope = rememberCoroutineScope()

    val images = viewModel.images

    val sheetPeekHeight by animateDpAsState(
        targetValue = if (images.isNotEmpty()) 60.dp else 0.dp,
        label = "sheetPeekHeight"
    )
    val isLoading by viewModel.isLoading
    val result by viewModel.result
    val error by viewModel.error

    var prompt by remember {
        mutableStateOf("")
    }

    val voiceRecognitionLauncher = rememberLauncherForActivityResult(
        contract = VoiceRecognitionContract(),
        onResult = { text ->
            if (text.isNotEmpty()) {
                prompt = text
                if (result.isNotEmpty() || !error.isNullOrEmpty()) {
                    viewModel.reset()
                }
                if (images.isEmpty()) {
                    capturePhoto(
                        context,
                        imageCapture,
                        onPhotoCaptured = { image ->
                            viewModel.addImage(image)
                            viewModel.sendPrompt(text)
                        },
                        onError = {
                            Toast
                                .makeText(
                                    context,
                                    it.localizedMessage,
                                    Toast.LENGTH_SHORT
                                )
                                .show()
                        }
                    )
                } else {
                    viewModel.sendPrompt(text)
                }
                coroutineScope.launch {
                    sheetScaffoldState.bottomSheetState.expand()
                }
            }
        }
    )

    var touchOffset: Offset? by remember {
        mutableStateOf(null)
    }

    BottomSheetScaffold(
        scaffoldState = sheetScaffoldState,
        sheetPeekHeight = sheetPeekHeight,
        sheetContent = {
            Column(modifier = Modifier.wrapContentHeight()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(images.size, key = { it }) { index ->
                        Image(
                            bitmap = images[index].asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .clickable {
                                    if (isLoading.not() && result.isEmpty()) {
                                        viewModel.removeImage(images[index])
                                    }
                                }
                                .animateItemPlacement()
                                .height(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }

                OutlinedTextField(
                    enabled = isLoading.not() && result.isEmpty(),
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = {
                        Text(text = "Enter your prompt here")
                    },
                    trailingIcon = {
                        IconButton(
                            enabled = isLoading.not(),
                            onClick = {
                                voiceRecognitionLauncher.launch(Unit)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )

                TextButton(
                    enabled = isLoading.not() && prompt.isNotBlank() && images.isNotEmpty() && result.isEmpty(),
                    onClick = {
                        viewModel.sendPrompt(prompt)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.textButtonColors(
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                ) {
                    Text(text = "Ask")
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (error != null) {
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }

                Text(
                    text = result,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .animateContentSize()
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
                AnimatedVisibility(visible = isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize(),
                factory = { previewView }
            )
            Box(modifier = Modifier
                .zIndex(2f)
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {},
                        onDoubleTap = {
                            touchOffset = it
                            if (result.isNotEmpty() || !error.isNullOrEmpty()) {
                                prompt = ""
                                viewModel.reset()
                            }
                            capturePhoto(
                                context = context,
                                imageCapture = imageCapture,
                                onPhotoCaptured = viewModel::addImage,
                                onError = {
                                    Toast
                                        .makeText(
                                            context,
                                            it.localizedMessage,
                                            Toast.LENGTH_SHORT
                                        )
                                        .show()
                                }
                            )
                        },
                        onLongPress = {
                            voiceRecognitionLauncher.launch(Unit)
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->

                        }
                    )
                }
            ) {
            }
        }

    }
}

private fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onPhotoCaptured: (Bitmap) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val mainExecutor: Executor = ContextCompat.getMainExecutor(context)

    imageCapture.takePicture(mainExecutor, object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(image: ImageProxy) {

            val matrix = Matrix().apply {
                postRotate(-image.imageInfo.rotationDegrees.toFloat())
                postScale(-1f, -1f)
            }
            val imageBitmap = image.toBitmap()

            val outputStream = ByteArrayOutputStream()

            val isCompressed = imageBitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)

            val byteArray = outputStream.toByteArray()

            val compressedBitmap: Bitmap = if (isCompressed) {
                BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            } else {
                imageBitmap
            }

            val rotatedBitmap = with(compressedBitmap) {
                Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
            }


            onPhotoCaptured(rotatedBitmap)
            image.close()
        }

        override fun onError(exception: ImageCaptureException) {
            onError(exception)
            Log.e("CameraContent", "Error capturing image", exception)
        }
    })
}