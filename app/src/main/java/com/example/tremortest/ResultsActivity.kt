package com.example.tremortest

import android.app.AlertDialog
import android.database.Cursor
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ResultsActivity : AppCompatActivity() {

    private lateinit var dbHelper: DbHelper
    private lateinit var tableLayout: TableLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_results)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dbHelper = DbHelper(this, null)
        tableLayout = findViewById(R.id.tableLayout)
        val clearButton: Button = findViewById(R.id.clearButton)
        clearButton.setOnClickListener{
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Удалить данные")
            builder.setMessage("Вы уверены, что хотите удалить все данные?")
            builder.setPositiveButton("Да") { dialog, which ->
                dbHelper.clearAllData()
                populateTable()
            }
            builder.setNegativeButton("Нет") { dialog, which ->
                dialog.dismiss()
            }
            val dialog: AlertDialog = builder.create()
            dialog.show()
        }

        populateTable()
    }

    private fun populateTable() {
        tableLayout.removeAllViews()
        val cursor: Cursor = dbHelper.getAllData()
        if (cursor.moveToFirst()) {
            val tableRowHeader = TableRow(this)
            val headers = arrayOf("№", "Дата, время", "Диагноз", "Частота (акс,Гц)", "Амплитуда (акс)",
                "Частота (гир,Гц)", "Амплитуда (гир)", "Комментарий")
            for (header in headers) {
                val textView = TextView(this)
                textView.text = header
                textView.setPadding(8, 8, 8, 8)
                textView.setTypeface(null, Typeface.BOLD)
                textView.textSize = 16f
                tableRowHeader.addView(textView)
            }
            tableLayout.addView(tableRowHeader)

            do {
                val tableRow = TableRow(this)
                val number = cursor.getInt(0)
                val date = cursor.getString(1)
                val diagnosis = cursor.getString(2)
                val accFrequency = cursor.getFloat(3)
                val accAmplitude = cursor.getFloat(4)
                val gyroFrequency = cursor.getFloat(5)
                val gyroAmplitude = cursor.getFloat(6)
                val commentary = cursor.getString(7)

                val columns = arrayOf(number.toString(), date, diagnosis,
                    String.format("%.3f", accFrequency),
                    String.format("%.3f", accAmplitude),
                    String.format("%.3f", gyroFrequency),
                    String.format("%.3f", gyroAmplitude), commentary)
                for (column in columns) {
                    val textView = TextView(this)
                    textView.text = column
                    textView.setPadding(8, 8, 8, 8)
                    textView.textSize = 14f
                    tableRow.addView(textView)
                }
                tableRow.setOnLongClickListener {
                    showDeleteDialog(number)
                    true
                }
                tableLayout.addView(tableRow)
            } while (cursor.moveToNext())
        }
        cursor.close()
    }

    private fun showDeleteDialog(number: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Удалить измерение")
        builder.setMessage("Вы уверены, что хотите удалить измерение $number?")
        builder.setPositiveButton("Да") { dialog, which ->
            dbHelper.deleteResult(number)
            populateTable()
        }
        builder.setNegativeButton("Нет") { dialog, which ->
            dialog.dismiss()
        }
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}