package com.electricdreams.numo.feature.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.electricdreams.numo.R
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Clean, fullscreen QR Code Scanner.
 * Minimal UI with just a viewfinder frame - no distracting animations.
 * 
 * Usage:
 * - Start with ActivityResultLauncher
 * - Check for RESULT_OK and get EXTRA_QR_VALUE from the result
 * - Optionally set EXTRA_TITLE and EXTRA_INSTRUCTION via intent extras
 */
class QRScannerActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "QRScanner"
        private const val REQUEST_CAMERA_PERMISSION = 1002
        
        const val EXTRA_QR_VALUE = "qr_value"
        const val EXTRA_TITLE = "title"
        const val EXTRA_INSTRUCTION = "instruction"
    }

    private lateinit var previewView: PreviewView
    private lateinit var viewfinderFrame: View
    private lateinit var titleText: TextView
    private lateinit var instructionText: TextView
    private lateinit var closeButton: ImageButton

    private lateinit var cameraExecutor: ExecutorService
    private var barcodeScanner: BarcodeScanner? = null
    private var isScanning = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge fullscreen
        enableFullscreen()
        
        setContentView(R.layout.activity_qr_scanner)

        initViews()
        setupCustomization()
        
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Configure scanner for QR codes only
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()

        barcodeScanner = BarcodeScanning.getClient(options)

        if (checkCameraPermission()) {
            startCamera()
        } else {
            requestCameraPermission()
        }
    }

    private fun enableFullscreen() {
        // Make activity fullscreen
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.statusBars())
        controller.hide(WindowInsetsCompat.Type.navigationBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        
        // Keep screen on while scanning
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun initViews() {
        previewView = findViewById(R.id.preview_view)
        viewfinderFrame = findViewById(R.id.viewfinder_frame)
        titleText = findViewById(R.id.title_text)
        instructionText = findViewById(R.id.instruction_text)
        closeButton = findViewById(R.id.close_button)

        closeButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
        
        // Subtle entrance animation
        viewfinderFrame.alpha = 0f
        viewfinderFrame.scaleX = 0.95f
        viewfinderFrame.scaleY = 0.95f
        viewfinderFrame.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(300)
            .start()
    }

    private fun setupCustomization() {
        intent.getStringExtra(EXTRA_TITLE)?.let { titleText.text = it }
        intent.getStringExtra(EXTRA_INSTRUCTION)?.let { instructionText.text = it }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            REQUEST_CAMERA_PERMISSION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.qr_scanner_permission_required),
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        if (!isScanning) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        @androidx.camera.core.ExperimentalGetImage
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )

                            barcodeScanner?.process(image)
                                ?.addOnSuccessListener { barcodes ->
                                    if (barcodes.isNotEmpty() && isScanning) {
                                        val barcode = barcodes.first()
                                        barcode.rawValue?.let { value ->
                                            isScanning = false
                                            onQRCodeDetected(value)
                                        }
                                    }
                                }
                                ?.addOnFailureListener { e ->
                                    Log.e(TAG, "QR scanning failed: ${e.message}")
                                }
                                ?.addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed: ${e.message}")
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun onQRCodeDetected(value: String) {
        runOnUiThread {
            // Haptic feedback
            previewView.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
            
            // Quick success pulse on viewfinder
            viewfinderFrame.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(100)
                .withEndAction {
                    val intent = Intent().apply {
                        putExtra(EXTRA_QR_VALUE, value)
                    }
                    setResult(RESULT_OK, intent)
                    finish()
                }
                .start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isScanning = false
        cameraExecutor.shutdown()
        barcodeScanner?.close()
    }
}
