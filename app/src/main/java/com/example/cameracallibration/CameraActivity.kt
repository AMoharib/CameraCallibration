package com.example.cameracallibration

import android.Manifest
import android.R.attr.data
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import com.example.cameracallibration.extensions.toByteArray
import com.example.cameracallibration.models.Coordinates
import com.example.cameracallibration.models.Item
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import java.io.File
import java.util.concurrent.Executor


class CameraActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sref: StorageReference
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

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
    private lateinit var gyroscopeValue: TextView

    private var mGravity: FloatArray = FloatArray(9) { 0f }
    private var mGeomagnetic: FloatArray = FloatArray(9) { 0f }

    private var azimuth: Float = 0.0f
    private var pitch: Float = 0.0f
    private var roll: Float = 0.0f


    companion object {
        private val REQUEST_CODE_PERMISSION = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)


        viewFinder = findViewById(R.id.viewFinder)
        gyroscopeValue = findViewById(R.id.gyroscopeValue)
        captureButton = findViewById(R.id.captureButton)

        auth = FirebaseAuth.getInstance()
        firestore = Firebase.firestore
        sref = FirebaseStorage.getInstance().reference

        if (allPermissionGranted()) {
            viewFinder.post {
                displayId = viewFinder.display.displayId
                startCameraCapture()
                startCamera()
            }
        } else {
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
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (allPermissionGranted()) {
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

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val cameraSelector =
                CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

            val preview = Preview.Builder()
                .setTargetRotation(rotation)
                .build()

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this as LifecycleOwner, cameraSelector, preview, imageCapture
                )

                preview.setSurfaceProvider(viewFinder.surfaceProvider)

            } catch (exc: Exception) {
                Log.d("MainActivity", exc.message)

            }
        }, cameraExecutor)
    }

    private fun startCameraCapture() {

        try {


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
        } catch (exc: Exception) {
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
        imageCapture?.takePicture(cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val byteArray: ByteArray = image.image!!.toByteArray()
                    uploadImage(byteArray)
                }

                override fun onError(exception: ImageCaptureException) {

                }
            })
//        imageCapture?.takePicture(outputFileOptions, cameraExecutor,
//            object : ImageCapture.OnImageSavedCallback {
//
//                override fun onImageSaved(outputFileResult: ImageCapture.OutputFileResults) {
//                    val intent = Intent()
//                    intent.action = Intent.ACTION_VIEW
//                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//                    val photoURI = FileProvider.getUriForFile(
//                        applicationContext,
//                        applicationContext.applicationContext.packageName.toString() + ".provider",
//                        file
//                    )
//                    intent.setDataAndType(photoURI, "image/*")
//                    startActivity(intent)
//
//
//                }
//
//                override fun onError(exception: ImageCaptureException) {
//                    Toast.makeText(
//                        applicationContext,
//                        "Something went wrong",
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
//            })
    }

    private fun uploadImage(byteArray: ByteArray) {

        val ref = sref.child("media/${System.currentTimeMillis()}.jpg")
        val uploadTask: UploadTask = ref.putBytes(byteArray)
        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            ref.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                saveData(downloadUri)
            } else {
                // Handle failures
                // ...
            }
        }
    }

    private fun saveData(imageUri: Uri?){
        val userId = auth.currentUser?.uid
        val coordinates = Coordinates(pitch, roll, azimuth)
        val item = Item(coordinates, imageUri.toString(), userId)

        firestore.collection("items").add(item).addOnCompleteListener {
            if(it.isSuccessful) {
                onBackPressed()
            }
        }


    }

    private fun allPermissionGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun registerSensors() {
        mSensorManger.registerListener(
            this,
            mAccelerometer,
            SensorManager.SENSOR_DELAY_NORMAL,
            SensorManager.SENSOR_DELAY_UI
        )
        mSensorManger.registerListener(
            this,
            mGyroscope,
            SensorManager.SENSOR_DELAY_NORMAL,
            SensorManager.SENSOR_DELAY_UI
        )
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
                azimuth = orientation[0]; // orientation contains: azimuth, pitch and roll
                pitch = orientation[1];
                roll = orientation[2];

                gyroscopeValue.text = "X: ${
                    Math.toDegrees(pitch.toDouble()).toInt()
                }\nY: ${
                    Math.toDegrees(roll.toDouble()).toInt()
                }\nZ: ${((Math.toDegrees(azimuth.toDouble()) + 360) % 360).toInt()}"
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        return
    }
}