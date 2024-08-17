package com.example.tremortest

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SaveResultsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_save_results)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val commentary: EditText = findViewById(R.id.commentaryEditView)
        val saveButton: Button = findViewById(R.id.saveButton)
        val dateTime = intent.getSerializableExtra("dateTime") as String
        val diagnosis = getDisease(intent.getSerializableExtra("diagnosis") as String)
        val accFrequency = intent.getSerializableExtra("accFrequency") as Double
        val gyroFrequency = intent.getSerializableExtra("gyroFrequency") as Double
        val accAmplitude = intent.getSerializableExtra("accAmplitude") as Double
        val gyroAmplitude = intent.getSerializableExtra("gyroAmplitude") as Double

        saveButton.setOnClickListener{
            val commentaryText = commentary.text.toString().trim()
            val result = Result(dateTime, diagnosis, gyroFrequency.toFloat(), gyroAmplitude.toFloat(),
                accFrequency.toFloat(), accAmplitude.toFloat(), commentaryText)
            val db = DbHelper(this, null)
            db.addResult(result)
            Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show()
            val resultIntent = Intent()
            resultIntent.putExtra(FinishTestActivity.RESULT_KEY, true)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun getDisease(diagnosis: String): String{
        return if(diagnosis.contains(" и "))
            "ЭТ/ПТ"
        else if(diagnosis.contains("паркинсонический"))
            "ПТ"
        else if(diagnosis.contains("эссенциальный"))
            "ЭТ"
        else "Здоров"
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}