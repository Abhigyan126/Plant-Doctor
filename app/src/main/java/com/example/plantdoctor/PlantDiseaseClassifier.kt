package com.example.plantdoctor

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class PlantDiseaseClassifier(private val context: Context) {
    private var interpreter: Interpreter? = null
    private val imageSize = 224
    private val numChannels = 3
    private val modelPath = "model.tflite"

    // Define class labels
    private val labels = listOf(
        "Pepper__bell___Bacterial_spot",
        "Pepper__bell___healthy",
        "Potato___Early_blight",
        "Potato___Late_blight",
        "Potato___healthy",
        "Tomato_Bacterial_spot",
        "Tomato_Early_blight",
        "Tomato_Late_blight",
        "Tomato_Leaf_Mold",
        "Tomato_Septoria_leaf_spot",
        "Tomato_Spider_mites_Two_spotted_spider_mite",
        "Tomato__Target_Spot",
        "Tomato__Tomato_YellowLeaf__Curl_Virus",
        "Tomato__Tomato_mosaic_virus",
        "Tomato_healthy"
    )

    init {
        setupInterpreter()
    }

    private fun setupInterpreter() {
        val modelBuffer = loadModelFile()
        val options = Interpreter.Options()
        interpreter = Interpreter(modelBuffer, options)
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, true)
        val byteBuffer = ByteBuffer.allocateDirect(imageSize * imageSize * numChannels * 4)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(imageSize * imageSize)
        scaledBitmap.getPixels(pixels, 0, imageSize, 0, 0, imageSize, imageSize)

        for (pixel in pixels) {
            // Extract RGB values
            val r = (pixel shr 16 and 0xFF)
            val g = (pixel shr 8 and 0xFF)
            val b = (pixel and 0xFF)

            // Normalize to [0, 1.0] range
            byteBuffer.putFloat(r / 255.0f)
            byteBuffer.putFloat(g / 255.0f)
            byteBuffer.putFloat(b / 255.0f)
        }

        return byteBuffer
    }

    fun classify(bitmap: Bitmap): String {
        val inputBuffer = preprocessImage(bitmap)
        val outputBuffer = Array(1) { FloatArray(labels.size) }

        interpreter?.run(inputBuffer, outputBuffer)

        val results = outputBuffer[0]
        val maxIndex = results.indices.maxByOrNull { results[it] } ?: 0

        return labels[maxIndex]
    }

    fun close() {
        interpreter?.close()
    }
}