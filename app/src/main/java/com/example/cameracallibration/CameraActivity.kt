package com.example.cameracallibration

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.droidnet.DroidListener
import com.droidnet.DroidNet
import com.example.cameracallibration.extensions.toBitmap
import com.example.cameracallibration.extensions.toByteArray
import com.example.cameracallibration.models.Coordinates
import com.example.cameracallibration.models.Item
import com.example.cameracallibration.models.ItemLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor


class CameraActivity : AppCompatActivity(), SensorEventListener, DroidListener {

    private lateinit var sref: StorageReference
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var fusedLocationClient: FusedLocationProviderClient


    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private lateinit var viewFinder: PreviewView

    private var displayId: Int = -1
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK

    private lateinit var cameraExecutor: Executor

    private lateinit var mSensorManger: SensorManager
    private lateinit var mAccelerometer: Sensor
    private lateinit var mGyroscope: Sensor

    // Views
    private lateinit var latText: TextView
    private lateinit var longText: TextView
    private lateinit var timeText: TextView
    private lateinit var azimuthText: TextView
    private lateinit var incText: TextView


    private lateinit var unitsSpinner: Spinner
    private lateinit var quantitySpinner: Spinner
    private lateinit var planesSpinner: Spinner

    private lateinit var captureButton: Button
    private lateinit var loading: ProgressBar

    private lateinit var imagePreview: ImageView
    private lateinit var greenLight: ImageView
    private lateinit var redLight: ImageView

    // Location
    private lateinit var currentLocation: Location

    // Connectivity
    private lateinit var mDroidNet: DroidNet


    private var mGravity: FloatArray = FloatArray(9) { 0f }
    private var mGeomagnetic: FloatArray = FloatArray(9) { 0f }

    private var azimuth: Float = 0.0f
    private var pitch: Float = 0.0f
    private var roll: Float = 0.0f

    private lateinit var imageByteArray: ByteArray
    private lateinit var selectedAirplane: String
    private lateinit var selectedUnit: String
    private lateinit var selectedQuantity: String


    companion object {
        private val REQUEST_CODE_PERMISSION = 10
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)

        DroidNet.init(this)

        // Views
        viewFinder = findViewById(R.id.viewFinder)
        imagePreview = findViewById(R.id.ivPreview)

        latText = findViewById(R.id.latText)
        longText = findViewById(R.id.longText)
        timeText = findViewById(R.id.timeText)
        azimuthText = findViewById(R.id.azimuthText)
        incText = findViewById(R.id.incText)

        unitsSpinner = findViewById(R.id.units_spinner)
        quantitySpinner = findViewById(R.id.quantity_spinner)
        planesSpinner = findViewById(R.id.planes_spinner)

        greenLight = findViewById(R.id.greenLight)
        redLight = findViewById(R.id.redLight)

        captureButton = findViewById(R.id.captureButton)

        loading = findViewById(R.id.loading)

        // Firebase
        auth = FirebaseAuth.getInstance()
        firestore = Firebase.firestore
        sref = FirebaseStorage.getInstance().reference

        // Location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Connectivity
        mDroidNet = DroidNet.getInstance();
        mDroidNet.addInternetConnectivityListener(this);


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
        setupTimer()
        setupSpinners()
        getCurrentLocation()

        captureButton.setOnClickListener {
            loading.visibility = View.VISIBLE
            takePhoto()
        }
    }

    private fun setupTimer() {
        val timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    timeText.text = "${SimpleDateFormat("hh:mm:ss aa").format(Date())}"
                }
            }
        }, 0, 1000)
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    currentLocation = location
                }
                latText.text = "Lat: ${location?.latitude}"
                longText.text = "Long: ${location?.longitude}"
            }
    }

    private fun saveData(imageUri: Uri?, location: Location?) {
        val userId = auth.currentUser?.uid
        val coordinates = Coordinates(pitch, roll, azimuth)
        val itemLocation = ItemLocation(location?.latitude, location?.longitude)
        val item = Item(coordinates, imageUri.toString(), userId, selectedUnit, selectedQuantity, selectedAirplane, itemLocation, System.currentTimeMillis())

        firestore.collection("items").add(item).addOnCompleteListener {
            if (it.isSuccessful) {
                loading.visibility = View.GONE
                onBackPressed()
            }
        }


    }

    private fun setupSpinners() {

        val airplanes = resources.getStringArray(R.array.airplanes)
        val units = resources.getStringArray(R.array.unit)
        val quantities = resources.getStringArray(R.array.quantity)

        selectedAirplane = airplanes[0]
        selectedUnit = units[0]
        selectedQuantity = quantities[0]

        ArrayAdapter.createFromResource(
            this,
            R.array.airplanes,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            planesSpinner.adapter = adapter
        }

        planesSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                selectedAirplane = airplanes[p2]
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

        }

        ArrayAdapter.createFromResource(
            this,
            R.array.unit,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            unitsSpinner.adapter = adapter
        }

        unitsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                selectedUnit = units[p2]
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }

        ArrayAdapter.createFromResource(
            this,
            R.array.quantity,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            quantitySpinner.adapter = adapter
        }

        quantitySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                selectedQuantity = units[p2]
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }
    }

    private fun uploadImage() {

        val ref = sref.child("media/${System.currentTimeMillis()}.jpg")
        val uploadTask: UploadTask = ref.putBytes(imageByteArray)
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
                saveData(downloadUri, currentLocation)
            } else {
                // Handle failures
                // ...
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
                .setTargetRotation(Surface.ROTATION_0)
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
                @SuppressLint("UnsafeOptInUsageError")
                override fun onCaptureSuccess(image: ImageProxy) {
                    imageByteArray = image.image!!.toByteArray()
                    viewFinder.visibility = View.GONE

                    imagePreview.setImageBitmap(image.toBitmap())
                    imagePreview.visibility = View.VISIBLE

                    uploadImage()
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

                azimuthText.text = "Azimuth: ${((Math.toDegrees(azimuth.toDouble()) + 360) % 360).toInt()}°"
                incText.text = "Inclination: ${((Math.toDegrees(roll.toDouble()) + 360) % 360).toInt()}°"

            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        return
    }

    override fun onInternetConnectivityChanged(isConnected: Boolean) {
        if(isConnected) {
            greenLight.alpha = 1f
            redLight.alpha = 0.2f
        } else {
            greenLight.alpha = 0.2f
            redLight.alpha = 1f
        }
    }

    private fun allPermissionGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (allPermissionGranted()) {
                getCurrentLocation()
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

    override fun onLowMemory() {
        super.onLowMemory()
        DroidNet.getInstance().removeAllInternetConnectivityChangeListeners();
    }

    override fun onDestroy() {
        super.onDestroy()
        mDroidNet.removeInternetConnectivityChangeListener(this);
    }
}