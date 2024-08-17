package com.example.tremortest

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.*
import com.github.psambit9791.jdsp.filter.Butterworth
import com.github.psambit9791.jdsp.signal.peaks.*
import com.github.psambit9791.jdsp.transform.FastFourier
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartModel
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartType
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartView
import com.github.aachartmodel.aainfographics.aachartcreator.AASeriesElement
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FinishTestActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_CODE = 1
        const val RESULT_KEY = "result_key"
    }

    private lateinit var accelerometerAxisX: DoubleArray
    private lateinit var accelerometerAxisY: DoubleArray
    private lateinit var accelerometerAxisZ: DoubleArray
    private lateinit var gyroscopeAxisX: DoubleArray
    private lateinit var gyroscopeAxisY: DoubleArray
    private lateinit var gyroscopeAxisZ: DoubleArray
    private var isSaved = false

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_finish_test)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val accelerometerData = intent.getSerializableExtra("accelerometerData") as Array<FloatArray>
        val gyroscopeData = intent.getSerializableExtra("gyroscopeData") as Array<FloatArray>
        val filterOrder = 3
        val cutoffFrequency = 20.0
        val samplingFrequency = 200.0

        accelerometerAxisX = getAxisData(accelerometerData, 0).drop(250).toDoubleArray()
        accelerometerAxisY = getAxisData(accelerometerData, 1).drop(250).toDoubleArray()
        accelerometerAxisZ = getAxisData(accelerometerData, 2).drop(250).toDoubleArray()
        gyroscopeAxisX = getAxisData(gyroscopeData, 0).drop(250).toDoubleArray()//.dropLast(3500).toDoubleArray()
        gyroscopeAxisY = getAxisData(gyroscopeData, 1).drop(250).toDoubleArray()//.dropLast(3500).toDoubleArray()
        gyroscopeAxisZ = getAxisData(gyroscopeData, 2).drop(250).toDoubleArray()//.dropLast(3500).toDoubleArray()

        val butterworthFilter = Butterworth(samplingFrequency)
        accelerometerAxisX = butterworthFilter.lowPassFilter(accelerometerAxisX, filterOrder, cutoffFrequency)
        accelerometerAxisY = butterworthFilter.lowPassFilter(accelerometerAxisY, filterOrder, cutoffFrequency)
        accelerometerAxisZ = butterworthFilter.lowPassFilter(accelerometerAxisZ, filterOrder, cutoffFrequency)
        gyroscopeAxisX = butterworthFilter.lowPassFilter(gyroscopeAxisX, filterOrder, cutoffFrequency)
        gyroscopeAxisY = butterworthFilter.lowPassFilter(gyroscopeAxisY, filterOrder, cutoffFrequency)
        gyroscopeAxisZ = butterworthFilter.lowPassFilter(gyroscopeAxisZ, filterOrder, cutoffFrequency)

        accelerometerAxisX = removeDCComponent(accelerometerAxisX)
        accelerometerAxisY = removeDCComponent(accelerometerAxisY)
        accelerometerAxisZ = removeDCComponent(accelerometerAxisZ)
        gyroscopeAxisX = removeDCComponent(gyroscopeAxisX)
        gyroscopeAxisY = removeDCComponent(gyroscopeAxisY)
        gyroscopeAxisZ = removeDCComponent(gyroscopeAxisZ)

        var (accFrequency, accAmplitude) = getFrequencyAndAmplitude(accelerometerAxisX,
            accelerometerAxisY, accelerometerAxisZ, samplingFrequency, true)
        var (gyroFrequency, gyroAmplitude) = getFrequencyAndAmplitude(gyroscopeAxisX,
            gyroscopeAxisY, gyroscopeAxisZ, samplingFrequency, false)

        val diagnosis = getDiagnosis(accFrequency, accAmplitude, gyroFrequency, gyroAmplitude)
        val textView: TextView = findViewById(R.id.resultView)
        textView.text = "${diagnosis}"

        displayAccChart()
        displayGyroChart()

        val currentTime = LocalDateTime.now()
        val dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        val dateTime = currentTime.format(dateTimeFormatter)

        val finishButton: Button = findViewById(R.id.finishButton)
        finishButton.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finishAffinity()
        }
        val saveButton: Button = findViewById(R.id.saveButton)
        saveButton.setOnClickListener{
            if(isSaved)
                Toast.makeText(this, "Данные уже сохранены", Toast.LENGTH_SHORT).show()
            else{
                val intent = Intent(this, SaveResultsActivity::class.java)
                intent.putExtra("dateTime", dateTime)
                intent.putExtra("diagnosis", diagnosis)
                intent.putExtra("accFrequency", accFrequency)
                intent.putExtra("gyroFrequency", gyroFrequency)
                intent.putExtra("accAmplitude", accAmplitude)
                intent.putExtra("gyroAmplitude", gyroAmplitude)
                startActivityForResult(intent, REQUEST_CODE)
            }
        }
    }

    private fun getAxisData(data: Array<FloatArray>, axis: Int): DoubleArray {
        val axisData = mutableListOf<Double>()
        for (tuple in data) {
            axisData.add(tuple[axis].toDouble())
        }
        return axisData.toDoubleArray()
    }

    private fun removeDCComponent(signal: DoubleArray): DoubleArray {
        val mean = signal.average()
        return signal.map { it - mean }.toDoubleArray()
    }

    private fun getFrequencyAndAmplitude(signalX: DoubleArray, signalY: DoubleArray,
                                         signalZ: DoubleArray, samplingFrequency: Double,
                                         isAccData: Boolean):
                                            Pair<Double, Double>{
        val spectrumX = getFFTSpectrum(signalX, isAccData)
        val spectrumY = getFFTSpectrum(signalY, isAccData)
        val spectrumZ = getFFTSpectrum(signalZ, isAccData)

        val L1Spectrum = mutableListOf<Double>()
        for (i in spectrumX.indices) {//объединение осей
            val magnitude = sqrt(spectrumX[i].pow(2) + spectrumY[i].pow(2) + spectrumZ[i].pow(2))
            L1Spectrum.add(magnitude)
        }

        val frequencyResolution = samplingFrequency / signalX.size //частотное разрешение
        val maxFrequencyIndex = (20 / frequencyResolution).toInt().coerceAtMost(L1Spectrum.size - 1)
        val limitedSpectrum = L1Spectrum.subList(0, maxFrequencyIndex + 1) //ограничение спектра

        val peaks = FindPeak(limitedSpectrum.toDoubleArray()).detectPeaks()
        val highestPeakIndex = peaks.heights.indexOfFirst { it == peaks.heights.maxOrNull() }
        val frequency = highestPeakIndex * frequencyResolution
        val amplitude = abs(L1Spectrum[highestPeakIndex])
        return Pair(frequency, amplitude)
    }

    private fun getFFTSpectrum(signal: DoubleArray, isAccData: Boolean): DoubleArray{
        var signalCopy = signal
        if(isAccData)
            signalCopy = signalToGScale(signalCopy)
        val fft = FastFourier(signalCopy)
        fft.transform()
        val spectrum = fft.getMagnitude(true)
        return spectrum.map { it / signal.size }.toDoubleArray() //нормализация
    }

    private fun signalToGScale(signal: DoubleArray): DoubleArray{
        val g = 9.81
        return signal.map { it / g }.toDoubleArray()
    }

    private fun getDiagnosis(accFrequency: Double, accAmplitude: Double,
                             gyroFrequency: Double, gyroAmplitude: Double): String{
        var diagnosis = "Подозрение на "
        val frequency = if(gyroAmplitude > 0.003 && accAmplitude > 0.001){
            if(accAmplitude > 0.003)
                accFrequency+gyroFrequency/2
            else
                gyroFrequency
        } else if(accAmplitude > 0.003 && gyroAmplitude > 0.001)
            accAmplitude
        else return "Нет подозрений на тремор"
        diagnosis += if(frequency in 4.0..6.0)
            "паркинсонический и эссенциальный тремор"
        else if(frequency in 3.0..4.0)
            "паркинсонический тремор"
        else if(frequency in 6.0..12.0)
            "эссенциальный тремор"
        else
            "тремор"
        return diagnosis
    }

    private fun displayAccChart(){
        val aaChartView = findViewById<AAChartView>(R.id.acc_chart_view)
        val aaChartModel : AAChartModel = AAChartModel()
            .chartType(AAChartType.Area)
            .backgroundColor("#E0D9C8")
            .title("Акселерометр")
            .legendEnabled(false)
            .xAxisVisible(false)
            //.yAxisVisible(false)
            .series(arrayOf(
                AASeriesElement()
                    .name("Ось X")
                    .data(accelerometerAxisX.toList().toTypedArray()),
                AASeriesElement()
                    .name("Ось Y")
                    .data(accelerometerAxisY.toList().toTypedArray()),
                AASeriesElement()
                    .name("Ось Z")
                    .data(accelerometerAxisZ.toList().toTypedArray())
            )
            )
        aaChartView.aa_drawChartWithChartModel(aaChartModel)
    }

    private fun displayGyroChart(){
        val aaChartView = findViewById<AAChartView>(R.id.gyro_chart_view)
        val aaChartModel : AAChartModel = AAChartModel()
            .chartType(AAChartType.Area)
            .backgroundColor("#E0D9C8")
            .title("Гироскоп")
            .legendEnabled(false)
            .xAxisVisible(false)
            //.yAxisVisible(false)
            .series(arrayOf(
                AASeriesElement()
                    .name("Ось X")
                    .data(gyroscopeAxisX.toList().toTypedArray()),
                AASeriesElement()
                    .name("Ось Y")
                    .data(gyroscopeAxisY.toList().toTypedArray()),
                AASeriesElement()
                    .name("Ось Z")
                    .data(gyroscopeAxisZ .toList().toTypedArray())
            )
            )
        aaChartView.aa_drawChartWithChartModel(aaChartModel)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.let {
                val result = it.getBooleanExtra(RESULT_KEY, false)
                isSaved = result
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }
}