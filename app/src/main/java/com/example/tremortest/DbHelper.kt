package com.example.tremortest

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DbHelper(val context: Context, val factory: SQLiteDatabase.CursorFactory?):
    SQLiteOpenHelper(context, "results", factory, 1) {

    override fun onCreate(db: SQLiteDatabase?) {
        val query = "CREATE TABLE results (number INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "date CHAR(16), diagnosis VARCHAR(6), accFrequency FLOAT, " +
                "accAmplitude FLOAT, gyroFrequency FLOAT, gyroAmplitude FLOAT," +
                "commentary VARCHAR(200))"
        db!!.execSQL(query)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS results")
        onCreate(db)
    }

    fun addResult(res: Result){
        val values = ContentValues()
        values.put("date", res.date)
        values.put("diagnosis", res.diagnosis)
        values.put("accFrequency", res.accFrequency)
        values.put("accAmplitude", res.accAmplitude)
        values.put("gyroFrequency", res.gyroFrequency)
        values.put("gyroAmplitude", res.gyroAmplitude)
        values.put("commentary", res.commentary)
        val db = this.writableDatabase
        db.insert("results", null, values)
        db.close()
    }

    fun deleteResult(number: Int) {
        val db = this.writableDatabase
        db.execSQL("DELETE FROM results WHERE number = $number")
        db.close()
    }

    fun getAllData(): Cursor{
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM results", null)
    }

    fun clearAllData() {
        val db = this.writableDatabase
        db.execSQL("DROP TABLE IF EXISTS results")
        onCreate(db)
    }
}