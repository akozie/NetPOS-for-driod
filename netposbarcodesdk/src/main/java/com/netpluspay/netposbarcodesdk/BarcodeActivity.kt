package com.netpluspay.netposbarcodesdk

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Size
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException

internal class BarcodeActivity : AppCompatActivity() {
    private lateinit var cameraPreviewView: PreviewView
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcode_sdk)
        cameraPreviewView = findViewById(R.id.camera_preview_view)
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        requestCamera()
    }

    companion object {
        internal fun startActivity(
            context: Context,
            resultLauncher: ActivityResultLauncher<Intent>
        ) {
            val intent = Intent(context, BarcodeActivity::class.java)
            resultLauncher.launch(intent)
        }

        @Deprecated(message = "use startActivity with result launcher")
        internal fun startActivity(context: Activity, requestCode: Int){
            val intent = Intent(context, BarcodeActivity::class.java)
            context.startActivityForResult(intent, requestCode)
        }
    }

    private fun requestCamera() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Toast.makeText(this, "Start camera under m", Toast.LENGTH_SHORT).show()
            startCamera()
            return
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
            startCamera()
        else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.CAMERA
                )
            )
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_REQUEST_CODE
                )
            else
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_REQUEST_CODE
                )
        }
    }

    private fun startCamera() {
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                bindCameraPreview(cameraProvider)
            } catch (e: ExecutionException) {
                Toast.makeText(this, "Error starting camera " + e.message, Toast.LENGTH_SHORT)
                    .show()
            } catch (e: InterruptedException) {
                Toast.makeText(this, "Error starting camera " + e.message, Toast.LENGTH_SHORT)
                    .show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraPreview(cameraProvider: ProcessCameraProvider) {
        cameraPreviewView.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        val preview = Preview.Builder().build()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
        preview.setSurfaceProvider(cameraPreviewView.surfaceProvider)

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(
            ContextCompat.getMainExecutor(this),
            QRCodeImageAnalyzer(object : QRCodeFoundListener {
                override fun onQrCodeFound(text: String) {
                    cameraProvider.unbindAll()
                    val data = Intent().apply {
                        putExtra(RESULT_CODE_TEXT, text)
                    }
                    setResult(RESULT_OK, data)
                    finish()
                }

                override fun onQrCodeNotFound() {

                }

            })
        )
        cameraProvider.bindToLifecycle(
            this as LifecycleOwner,
            cameraSelector,
            imageAnalysis,
            preview
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.size == 1 && grantResults.first() == PackageManager.PERMISSION_GRANTED)
                startCamera()
            else {
                Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}