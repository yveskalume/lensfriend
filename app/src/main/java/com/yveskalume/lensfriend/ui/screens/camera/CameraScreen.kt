package com.yveskalume.lensfriend.ui.screens.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import android.widget.Toast
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CameraScreen(viewModel: CameraViewModel = viewModel()) {

    val context: Context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    val cameraController: LifecycleCameraController =
        remember { LifecycleCameraController(context) }

    val images = viewModel.images

    val sheetPeekHeight by animateDpAsState(
        targetValue = if (images.isNotEmpty()) 60.dp else 0.dp,
        label = "sheetPeekHeight"
    )

    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true
    )

    val sheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = sheetState
    )

    val coroutineScope = rememberCoroutineScope()

    var question by remember {
        mutableStateOf("")
    }

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
                                .height(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }

                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = {
                        Text(text = "Enter your prompt here")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )

                TextButton(
                    onClick = { /*TODO*/ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                ) {
                    Text(text = "Ask")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding()
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize(),
                factory = { mContext ->
                    PreviewView(mContext).apply {
                        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                        setBackgroundColor(android.graphics.Color.BLACK)
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_START
                    }.also { previewView ->
                        previewView.controller = cameraController
                        cameraController.bindToLifecycle(lifecycleOwner)
                    }
                }
            )
            Box(modifier = Modifier
                .zIndex(2f)
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {},
                        onDoubleTap = {
                            capturePhoto(
                                context = context,
                                cameraController = cameraController,
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
                            if (images.isNotEmpty()) {
                                coroutineScope.launch {
                                    sheetState.expand()
                                }
                            }
                        }
                    )
                }
            ) {
                if (touchOffset != null) {
                    Surface(
                        shape = CircleShape,
                        border = BorderStroke(width = 1.dp, color = Color.White),
                        modifier = Modifier
                            .size(80.dp)
                            .offset(touchOffset!!.x.dp, touchOffset!!.y.dp)

                    ) {

                    }
                }
            }
        }

    }
}

private fun capturePhoto(
    context: Context,
    cameraController: LifecycleCameraController,
    onPhotoCaptured: (Bitmap) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val mainExecutor: Executor = ContextCompat.getMainExecutor(context)

    cameraController.takePicture(mainExecutor, object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(image: ImageProxy) {

            val matrix = Matrix().apply {
                postRotate(-image.imageInfo.rotationDegrees.toFloat())
                postScale(-1f, -1f)
            }
            val imageBitmap = image.toBitmap()

            val correctedBitmap: Bitmap = with(imageBitmap) {
                Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
            }


            onPhotoCaptured(correctedBitmap)
            image.close()
        }

        override fun onError(exception: ImageCaptureException) {
            onError(exception)
            Log.e("CameraContent", "Error capturing image", exception)
        }
    })
}