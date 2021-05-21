package com.example.cameracallibration

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Half.EPSILON
import android.util.Log
import android.widget.TextView
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var mSensorManger: SensorManager
    private lateinit var mAccelerometer: Sensor
    private lateinit var mGyroscope: Sensor
    private lateinit var accelerometerValue: TextView
    private lateinit var gyroscopeValue: TextView

    // Create a constant to convert nanoseconds to seconds.
    private val NS2S = 1.0f / 1000000000.0f
    private val deltaRotationVector = FloatArray(4) { 0f }
    private var timestamp: Float = 0f

    private var mGravity: FloatArray = FloatArray(9) { 0f }
    private var mGeomagnetic: FloatArray = FloatArray(9) { 0f }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        accelerometerValue = findViewById(R.id.accelerometerValue)
        gyroscopeValue = findViewById(R.id.gyroscopeValue)
        mSensorManger = getSystemService(SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManger.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mGyroscope = mSensorManger.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        registerSensors()

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