package com.dec.app

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import android.graphics.Color
import android.view.Gravity

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Creating a simple screen without any XML files to prevent crashes!
        val textView = TextView(this)
        textView.text = "🤖 DEC IS ALIVE!\n\n1. Go to Phone Settings\n2. Open Accessibility\n3. Turn ON Dec\n4. Open Claude & Press Volume UP"
        textView.textSize = 20f
        textView.setTextColor(Color.BLACK)
        textView.setBackgroundColor(Color.WHITE)
        textView.gravity = Gravity.CENTER
        textView.setPadding(50, 50, 50, 50)
        
        setContentView(textView)
    }
}
