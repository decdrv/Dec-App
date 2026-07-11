package com.dec.app

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Start Dec's background engine
        val workRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
        WorkManager.getInstance(applicationContext).enqueue(workRequest)
        
        // 2. Show a quick message on the screen so you know it worked
        Toast.makeText(this, "Dec Engine Started!", Toast.LENGTH_LONG).show()
        
        // 3. Instantly close the screen and become invisible
        finish()
    }
}
