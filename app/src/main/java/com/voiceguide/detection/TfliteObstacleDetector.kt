package com.voiceguide.detection

import android.content.Context
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter

class TfliteObstacleDetector(
    context: Context,
    modelAssetName: String = "obstacle_detector.tflite"
) : ObstacleDetector {
    private val interpreter: Interpreter
    private val inputWidth: Int
    private val inputHeight: Int
    private val outputShape: IntArray
    private val postprocessor: YoloPostprocessor
    private val inputValues: FloatArray
    private val inputBuffer: ByteBuffer
    private val outputBuffer: ByteBuffer
    private val outputValues: FloatArray
    private var closed = false

    init {
        val modelBytes = context.assets.open(modelAssetName).use { input ->
            input.readBytes()
        }
        val modelBuffer = ByteBuffer.allocateDirect(modelBytes.size)
            .order(ByteOrder.nativeOrder())
            .put(modelBytes)
        modelBuffer.rewind()
        interpreter = Interpreter(modelBuffer)

        val inputTensorInfo = interpreter.getInputTensor(0)
        require(inputTensorInfo.dataType() == DataType.FLOAT32) { "Only FLOAT32 YOLO TFLite inputs are supported." }
        val inputShape = inputTensorInfo.shape()
        require(inputShape.size == 4 && inputShape[0] == 1 && inputShape[3] == 3) {
            "Expected input shape [1, height, width, 3]."
        }
        inputHeight = inputShape[1]
        inputWidth = inputShape[2]
        outputShape = interpreter.getOutputTensor(0).shape()
        require(interpreter.getOutputTensor(0).dataType() == DataType.FLOAT32) {
            "Only FLOAT32 YOLO TFLite outputs are supported."
        }
        postprocessor = YoloPostprocessor(inputWidth = inputWidth, inputHeight = inputHeight)
        inputValues = FloatArray(inputWidth * inputHeight * 3)
        inputBuffer = ByteBuffer.allocateDirect(inputValues.size * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
        outputValues = FloatArray(outputShape.product())
        outputBuffer = ByteBuffer.allocateDirect(outputValues.size * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
    }

    @Synchronized
    override fun detect(frame: FrameInput): DetectionResult {
        if (closed) {
            return DetectionResult(obstacles = emptyList(), metrics = frame.performance)
        }

        val totalStartNanos = System.nanoTime()

        val inputStartNanos = System.nanoTime()
        Nv21TensorConverter.writeFloatRgbTensor(
            frame = frame,
            targetWidth = inputWidth,
            targetHeight = inputHeight,
            output = inputValues
        )
        inputBuffer.rewind()
        inputValues.forEach { inputBuffer.putFloat(it) }
        inputBuffer.rewind()
        val inputTransformMillis = elapsedMillis(inputStartNanos)

        val inferenceStartNanos = System.nanoTime()
        outputBuffer.rewind()
        interpreter.run(inputBuffer, outputBuffer)
        val inferenceMillis = elapsedMillis(inferenceStartNanos)

        outputBuffer.rewind()
        for (index in outputValues.indices) {
            outputValues[index] = outputBuffer.float
        }

        val postprocessStartNanos = System.nanoTime()
        val detections = postprocessor.postprocess(outputValues, outputShape)
        val postprocessMillis = elapsedMillis(postprocessStartNanos)

        return DetectionResult(
            obstacles = detections,
            metrics = frame.performance.copy(
                inputTransformMillis = inputTransformMillis,
                inferenceMillis = inferenceMillis,
                postprocessMillis = postprocessMillis,
                totalMillis = elapsedMillis(totalStartNanos),
                detectedObstacleCount = detections.size
            )
        )
    }

    @Synchronized
    override fun close() {
        if (!closed) {
            interpreter.close()
            closed = true
        }
    }

    private fun IntArray.product(): Int {
        return fold(1) { total, value -> total * value }
    }

    private fun elapsedMillis(startNanos: Long): Long {
        return (System.nanoTime() - startNanos) / 1_000_000L
    }
}
