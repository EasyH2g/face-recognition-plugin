package com.h2g.face.plugin.face_recognition

import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.h2g.face.plugin.face_recognition.databinding.FaceRecognitionBinding
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FaceRecognitionActivity : AppCompatActivity() {
    private val TAG = "FaceRecognitionActivity"
    lateinit var binding: FaceRecognitionBinding
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private lateinit var cameraExecutor: ExecutorService
    private var facingStartTime = 0L
    private lateinit var faceDetector: FaceDetector
    private var stage = Step.INIT
    lateinit var preview: Preview

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FaceRecognitionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.back.setOnClickListener {
            setResult(RESULT_OK, intent)
            finish()
        }
        faceAnalysis({
            setResult(RESULT_OK, intent)
            finish()
        })
    }

    @OptIn(ExperimentalGetImage::class)
    private fun faceAnalysis(finishFaceAnalyse: () -> Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            cameraExecutor = Executors.newSingleThreadExecutor()
            faceDetector = FaceDetection.getClient(
                FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .setMinFaceSize(0.15f)
                    .build()
            )

            imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )
                            faceDetector.process(image)
                                .addOnSuccessListener { faces ->
                                    faces.processFaceAnalysis(finishFaceAnalyse)
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        }
                    }
                }
            try {
                camera = cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageCapture, imageAnalyzer
                )
            } catch (e: Exception) {
                Log.e(TAG, "faceAnalysis.error: $e")
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun List<Face>.processFaceAnalysis(finishFaceAnalyse: () -> Unit) {
        if (this.isEmpty()) {
            binding.message.text = getString(R.string.squarely_facing)
            stage = Step.INIT
            return
        }
        if (stage == Step.FINISH) {
            return
        }
        val faceCount = this.size
        if (faceCount > 1) {
            binding.message.text = getString(R.string.only_one_face)
            stage = Step.INIT
        }
        val face = this.first()
        if (stage == Step.INIT) {
            binding.message.text = getString(R.string.start)
            stage = Step.FIRST
            facingStartTime = System.currentTimeMillis()
        }
        when (stage) {
            Step.FIRST -> {
                if (!isFacingCamera(face)) {
                    binding.message.text = getString(R.string.squarely_facing)
                    stage = Step.INIT
                    return
                }
                if (System.currentTimeMillis() - facingStartTime >= 1000L) {
                    stage = Step.SECOND
                    takePhoto("1_") {
                        intent.putExtra(CommonConstant.FACE_FIRST_URL, it.absolutePath)
                    }
                }
            }

            Step.SECOND -> {
                binding.message.text = getString(R.string.please_smile)
                if (isSmiling(face)) {
                    takePhoto("2_") {
                        intent.putExtra(CommonConstant.FACE_SECOND_URL, it.absolutePath)
                    }
                    stage = Step.THIRD
                    binding.message.text = getString(R.string.slowly_shake)
                }
            }

            Step.THIRD -> {
                if (face.headEulerAngleY > 18.3f || face.headEulerAngleY < -18.4f) {
                    stage = Step.FINISH
                    takePhoto("3_") {
                        intent.putExtra(CommonConstant.FACE_THIRD_URL, it.absolutePath)
                        finishFaceAnalyse()
                    }
                }
            }

            else -> {}
        }
    }

    private fun isFacingCamera(face: Face?): Boolean {
        face ?: return false
        val thresholdX = 20f
        val thresholdY = 12f
        val thresholdZ = 8f
        return face.headEulerAngleZ < thresholdZ && face.headEulerAngleZ > -thresholdZ
                && face.headEulerAngleY < thresholdY && face.headEulerAngleY > -thresholdY
                && face.headEulerAngleX < thresholdX && face.headEulerAngleX > -thresholdX
    }

    private fun isSmiling(face: Face?): Boolean {
        return (face?.smilingProbability ?: 0f) > 0.7f
    }

    private fun takePhoto(child: String, onSaved: (File) -> Unit) {
        val file = File(cacheDir, child)
        imageCapture?.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(e: ImageCaptureException) {
                    Log.e(TAG, "takePhoto.onError: $e")
                    e.printStackTrace()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d(TAG, "takePhoto.onImageSaved: ${output.savedUri}")
                    onSaved(file)
                }
            }
        )
    }
}