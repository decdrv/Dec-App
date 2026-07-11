package com.dec.app

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.util.Log

class SyncWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        // This is where Dec will do the background work (Telegram/GDrive)
        Log.d("DEC_APP", "Dec Background Engine is running!")
        
        return Result.success()
    }
}
