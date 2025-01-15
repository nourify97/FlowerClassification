package com.nourify.flowerclassification.ui.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.nourify.flowerclassification.ml.FlowerModel
import com.nourify.flowerclassification.ui.model.Recognition
import com.nourify.flowerclassification.ui.utils.YuvToRgbConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.model.Model

class FlowerAnalyser(
    private val context: Context,
    private val onResultProcessed: (List<Recognition>) -> Unit,
) : ImageAnalysis.Analyzer {
    private val yuvToRgbConverter = YuvToRgbConverter(context)
    private lateinit var bitmapBuffer: Bitmap
    private lateinit var rotationMatrix: Matrix

    companion object {
        const val THROTTLE_TIMEOUT_MS = 2_000L
        const val MAX_RESULT_DISPLAY = 3 // Maximum number of results displayed
    }

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val flowerModel: FlowerModel by lazy {

        // TODO 6. Optional GPU acceleration
        val compatList = CompatibilityList()

        val options =
            if (compatList.isDelegateSupportedOnThisDevice) {
                Log.d(this.javaClass.name, "This device is GPU Compatible ")
                Model.Options
                    .Builder()
                    .setDevice(Model.Device.GPU)
                    .build()
            } else {
                Log.d(this.javaClass.name, "This device is GPU Incompatible ")
                Model.Options
                    .Builder()
                    .setNumThreads(4)
                    .build()
            }

        // Initialize the Flower Model
        FlowerModel.newInstance(context, options)
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        Log.d(this.javaClass.name, "start detecting")

        scope
            .launch {
                val tfImage = TensorImage.fromBitmap(toBitmap(imageProxy))

                val result =
                    flowerModel
                        .process(tfImage)
                        .probabilityAsCategoryList
                        .apply {
                            sortByDescending { it.score } // Sort with highest confidence first
                        }.take(MAX_RESULT_DISPLAY) // take the top results
                        .map { Recognition(it.label, it.score) }

                onResultProcessed(result)
                delay(THROTTLE_TIMEOUT_MS)
            }.invokeOnCompletion { exception ->
                exception?.printStackTrace()
                imageProxy.close()
            }
    }

    @OptIn(ExperimentalGetImage::class)
    @SuppressLint("UnsafeExperimentalUsageError")
    private fun toBitmap(imageProxy: ImageProxy): Bitmap? {
        val image = imageProxy.image ?: return null

        // Initialise Buffer
        if (!::bitmapBuffer.isInitialized) {
            // The image rotation and RGB image buffer are initialized only once
            Log.d(this.javaClass.name, "Initalise toBitmap()")
            rotationMatrix = Matrix()
            rotationMatrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            bitmapBuffer =
                Bitmap.createBitmap(
                    imageProxy.width,
                    imageProxy.height,
                    Bitmap.Config.ARGB_8888,
                )
        }

        // Pass image to an image analyser
        yuvToRgbConverter.yuvToRgb(image, bitmapBuffer)

        // Create the Bitmap in the correct orientation
        return Bitmap.createBitmap(
            bitmapBuffer,
            0,
            0,
            bitmapBuffer.width,
            bitmapBuffer.height,
            rotationMatrix,
            false,
        )
    }
}
