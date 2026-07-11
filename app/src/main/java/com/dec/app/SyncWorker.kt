package com.dec.app

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.net.URL

class SyncWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        try {
            // 1. Put your secret token inside the quotation marks below!
            val botToken = "8784363034:AAFtYXiZweUx5ocTl0zU-wysdeQoaOTHSLI"
            
            // 2. Ask Telegram if the Boss sent a message
            val getUrl = "https://api.telegram.org/bot$botToken/getUpdates"
            val response = URL(getUrl).readText()
            
            // 3. Read the data to find your personal Chat ID
            val json = JSONObject(response)
            val resultArray = json.getJSONArray("result")
            
            if (resultArray.length() > 0) {
                // Get the very last message you sent
                val lastMessage = resultArray.getJSONObject(resultArray.length() - 1)
                val chatId = lastMessage.getJSONObject("message").getJSONObject("chat").getString("id")
                
                // 4. Send a message BACK to your phone via Telegram!
                val message = "Hello Boss! Dec's background engine is officially connected to Telegram!"
                val sendUrl = "https://api.telegram.org/bot$botToken/sendMessage?chat_id=$chatId&text=$message"
                URL(sendUrl).readText()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Result.success()
    }
}
