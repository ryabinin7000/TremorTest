package com.example.tremortest

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MeasurementActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var accelerometerData = mutableListOf<FloatArray>()
    private var accelerometerRawData = mutableListOf<FloatArray>()
    private var gyroscopeData = mutableListOf<FloatArray>()
    private var gyroscopeRawData = mutableListOf<FloatArray>()
    private val defaultAccelerometerValues = FloatArray(3) { 0f }
    private val defaultGyroscopeValues = FloatArray(3) { 0f }
    private var timer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_measurement)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        if (accelerometer == null) {
            Toast.makeText(this, "Не найден акселерометр", Toast.LENGTH_SHORT).show()
            finish()
        }
        else if(gyroscope == null){
            Toast.makeText(this, "Не найден гироскоп", Toast.LENGTH_SHORT).show()
            finish()
        }

        accelerometer?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroscope?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }

        startTimer()
    }

    private fun startTimer(){
        val textView: TextView = findViewById(R.id.measurementTextView)
        textView.text = "Приготовьтесь..."
        val timerView: TextView = findViewById(R.id.timerView)
        timer = object : CountDownTimer(3000, 1000){
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished/1000+1
                timerView.text = "$secondsRemaining"
            }

            override fun onFinish() {
                startRecording()
            }
        }
        timer?.start()
    }

    private fun startRecording() {
        val textView: TextView = findViewById(R.id.measurementTextView)
        textView.text = "Идет запись"
        val timerView: TextView = findViewById(R.id.timerView)
        timerView.text = ""
        timer = object : CountDownTimer(5500, 4) { //freq 5000/4
            override fun onTick(millisUntilFinished: Long) {
                val currentAccValues = getCurrentAccelerometerValues()
                accelerometerData.add(currentAccValues)
                val currentGyroValues = getCurrentGyroscopeValues()
                gyroscopeData.add(currentGyroValues)
                //val secondsRemaining = millisUntilFinished / 1000 + 1
                //timerView.text = "$secondsRemaining"
            }

            override fun onFinish() {
                val intent = Intent(this@MeasurementActivity, FinishTestActivity::class.java)
                intent.putExtra("accelerometerData",accelerometerData.toTypedArray())
                intent.putExtra("gyroscopeData",gyroscopeData.toTypedArray())
                startActivity(intent)
            }
        }
        timer?.start()
    }

    private fun getCurrentAccelerometerValues(): FloatArray {
        return accelerometerRawData.removeFirstOrNull() ?: defaultAccelerometerValues
    }

    private fun getCurrentGyroscopeValues(): FloatArray {
        return gyroscopeRawData.removeFirstOrNull() ?: defaultGyroscopeValues
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val values = event.values.clone()
            accelerometerRawData.add(values)
        }
        if (event?.sensor?.type == Sensor.TYPE_GYROSCOPE) {
            val values = event.values.clone()
            gyroscopeRawData.add(values)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finishAffinity()
        timer?.cancel()
    }
}