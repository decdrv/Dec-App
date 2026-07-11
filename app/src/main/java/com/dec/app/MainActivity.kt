package com.dec.app

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Tell Dec to wake up automatically every 15 minutes forever
        val workRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES).build()
        
        // 2. Register this automatic loop deep inside the Android system
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "DecAutoSync",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
        
        // 3. Show a message and hide the screen
        Toast.makeText(this, "Dec Auto-Engine is now running forever!", Toast.LENGTH_LONG).show()
        finish()
    }
}
