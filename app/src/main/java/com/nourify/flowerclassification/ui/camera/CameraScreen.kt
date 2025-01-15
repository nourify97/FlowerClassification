package com.nourify.flowerclassification.ui.camera

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.camera.core.AspectRatio
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.nourify.flowerclassification.ui.model.Recognition

@Composable
fun CameraScreen() {
    CameraContent()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraContent() {
    val context: Context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    val cameraController: LifecycleCameraController = remember { LifecycleCameraController(context) }
    val flowersResult = remember { mutableStateOf<List<Recognition>>(emptyList()) }

    fun onResultUpdated(result: List<Recognition>) {
        Log.d("CameraContent", "Result from function: ${result.size}")
        flowersResult.value = result
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("Flower classifier") }) },
    ) { paddingValues: PaddingValues ->
        Column(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
        ) {
            AndroidView(
                modifier =
                    Modifier
                        .weight(8f),
                factory = { context ->
                    PreviewView(context)
                        .apply {
                            layoutParams =
                                LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                )
                            setBackgroundColor(Color.BLACK)
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            scaleType = PreviewView.ScaleType.FILL_START
                        }.also { previewView ->
                            startTextRecognition(
                                context = context,
                                cameraController = cameraController,
                                lifecycleOwner = lifecycleOwner,
                                previewView = previewView,
                                onResultUpdated = ::onResultUpdated,
                            )
                        }
                },
            )

            FlowerClassification(
                modifier =
                    Modifier
                        .weight(2f)
                        .fillMaxWidth()
                        .background(androidx.compose.ui.graphics.Color.White)
                        .padding(12.dp),
                result = flowersResult.value,
            )
        }
    }
}

@Composable
fun FlowerClassification(
    modifier: Modifier = Modifier,
    result: List<Recognition>,
) {
    @Composable
    fun Flower(
        modifier: Modifier = Modifier,
        label: String,
        confidence: String,
    ) {
        Row(
            modifier =
                modifier
                    .fillMaxWidth(),
        ) {
            Text(text = label)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = confidence)
        }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(0.dp),
    ) {
        items(result) { flower ->
            Flower(modifier, flower.label, flower.confidence.toString())
        }
    }
}

private fun startTextRecognition(
    context: Context,
    cameraController: LifecycleCameraController,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    onResultUpdated: (List<Recognition>) -> Unit,
) {
    cameraController.imageAnalysisTargetSize = CameraController.OutputSize(AspectRatio.RATIO_16_9)
    cameraController.setImageAnalysisAnalyzer(
        ContextCompat.getMainExecutor(context),
        FlowerAnalyser(context = context, onResultProcessed = onResultUpdated),
    )

    cameraController.bindToLifecycle(lifecycleOwner)
    previewView.controller = cameraController
}
