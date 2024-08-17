package com.example.tremortest

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val testButton: Button = findViewById(R.id.testButton)
        val resultsButton: Button = findViewById(R.id.resultsButton)
        val exitButton: Button = findViewById(R.id.exitButton)

        testButton.setOnClickListener{
            val intent = Intent(this, TestActivity::class.java)
            startActivity(intent)
        }
        resultsButton.setOnClickListener{
            val intent = Intent(this, ResultsActivity::class.java)
            startActivity(intent)
        }
        exitButton.setOnClickListener{
            finish()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}