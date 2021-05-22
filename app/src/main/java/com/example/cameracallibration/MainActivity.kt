package com.example.cameracallibration

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Half.EPSILON
import android.util.Log
import android.util.Size
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.lang.Exception
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private lateinit var viewFinder: PreviewView
    private lateinit var captureButton: FloatingActionButton

    private var displayId: Int = -1
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK

    private lateinit var cameraExecutor: Executor

    private lateinit var mSensorManger: SensorManager
    private lateinit var mAccelerometer: Sensor
    private lateinit var mGyroscope: Sensor
    private lateinit var accelerometerValue: TextView
    private lateinit var gyroscopeValue: TextView

    private var mGravity: FloatArray = FloatArray(9) { 0f }
    private var mGeomagnetic: FloatArray = FloatArray(9) { 0f }


    companion object {
        private val REQUEST_CODE_PERMISSION = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewFinder = findViewById(R.id.viewFinder)
        accelerometerValue = findViewById(R.id.accelerometerValue)
        gyroscopeValue = findViewById(R.id.gyroscopeValue)
        captureButton = findViewById(R.id.captureButton)

        if(allPermissionGranted()) {
            viewFinder.post {
                displayId = viewFinder.display.displayId
                startCameraCapture()
                startCamera()
            }
        } else{
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSION
            )
        }

        cameraExecutor = ContextCompat.getMainExecutor(this)

        mSensorManger = getSystemService(SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManger.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mGyroscope = mSensorManger.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        registerSensors()
        captureButton.setOnClickListener { takePhoto() }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode == REQUEST_CODE_PERMISSION) {
            if(allPermissionGranted()) {
                startCameraCapture()
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permission not granted by user",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        val rotation = viewFinder.display.rotation

        cameraProviderFuture.addListener(Runnable{
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

            val preview = Preview.Builder()
                .setTargetRotation(rotation)
                .build()

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this as LifecycleOwner, cameraSelector, preview, imageCapture
                )

                preview.setSurfaceProvider(viewFinder.surfaceProvider)

            } catch (exc: Exception){
                Log.d("MainActivity", exc.message)

            }
        }, cameraExecutor)
    }

    private fun startCameraCapture() {

        try{


        // SETUP CAPTURE MODE
        // to optimize photo capture for quality
        val captureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY


        // SETUP FLASH MODE

        // flash will be used according to the camera system's determination
        val flashMode = ImageCapture.FLASH_MODE_AUTO

        // SETUP ASPECT RATIO
        // 16:9 standard aspect ratio
//        val aspectRatio = AspectRatio.RATIO_16_9

        // 4:3 standard aspect ratio (default)
        val aspectRatio = AspectRatio.RATIO_4_3

        // SETUP TARGET RESOLUTION
        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
        val screenSize = Size(metrics.widthPixels, metrics.heightPixels)

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(captureMode)
            .setFlashMode(flashMode)
            .setTargetResolution(screenSize)
            .setTargetName("CameraConference")
            .build()
        }catch (exc: Exception) {
            Log.d("MainActivity", exc.message)

        }
    }

    private fun takePhoto() {
        val file = File(
            externalMediaDirs.first(),
            "${System.currentTimeMillis()}.jpg"
        )
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        Log.d("MainActivity", "Take Photo")
        imageCapture?.takePicture(outputFileOptions, cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(outputFileResult: ImageCapture.OutputFileResults) {
                    Toast.makeText(
                        applicationContext,
                        outputFileResult.savedUri.toString(),
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        applicationContext,
                        "Something went wrong",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun allPermissionGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun registerSensors() {
        mSensorManger.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI)
        mSensorManger.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {


            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER)
                mGravity = event.values;

            if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD)
                mGeomagnetic = event.values;

            val R = FloatArray(9) { 0f };
            val I = FloatArray(9) { 0f };

            val success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                val orientation = FloatArray(3) { 0f };

                SensorManager.getOrientation(R, orientation);
                val azimuth = orientation[0]; // orientation contains: azimuth, pitch and roll
                val pitch = orientation[1];
                val roll = orientation[2];

                gyroscopeValue.text = "X: ${Math.toDegrees(pitch.toDouble()).toInt()}\nY: ${Math.toDegrees(roll.toDouble()).toInt()}\n Z: ${(( Math.toDegrees(azimuth.toDouble()) + 360 ) % 360).toInt()}"
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        return
    }
}