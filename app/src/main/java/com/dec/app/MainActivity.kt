package com.dec.app

import android.app.Activity // 💡 THE FIX: Using Core Activity instead of AppCompatActivity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.graphics.Color
import android.view.Gravity

class MainActivity : Activity() { // 💡 THE FIX: Inheriting from Activity
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val mContext = this // Save context safely

        // 🎨 HACKER THEME UI (Programmatic Layout)
        val layout = LinearLayout(mContext)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 100, 50, 50)
        layout.setBackgroundColor(Color.parseColor("#0D0D0D")) // Deep Black
        layout.gravity = Gravity.CENTER_HORIZONTAL

        // 🟢 TITLE
        val title = TextView(mContext)
        title.text = "DEC OMNI-ENGINE"
        title.textSize = 28f
        title.setTextColor(Color.parseColor("#00FF00")) // Hacker Green
        title.gravity = Gravity.CENTER
        title.setTypeface(null, android.graphics.Typeface.BOLD)
        layout.addView(title)

        // ⚪ SUBTITLE
        val subtitle = TextView(mContext)
        subtitle.text = "Autonomous AI Parasite\nStatus: ONLINE"
        subtitle.textSize = 14f
        subtitle.setTextColor(Color.LTGRAY)
        subtitle.gravity = Gravity.CENTER
        subtitle.setPadding(0, 10, 0, 80)
        layout.addView(subtitle)

        // 📝 TOPIC INPUT BOX
        val topicInput = EditText(mContext)
        topicInput.hint = "Enter Topic (e.g. Quantum Physics, Hacking)"
        topicInput.setHintTextColor(Color.DKGRAY)
        topicInput.setTextColor(Color.WHITE)
        topicInput.setBackgroundColor(Color.parseColor("#1A1A1A"))
        topicInput.setPadding(40, 40, 40, 40)
        layout.addView(topicInput)

        // 🚀 START BUTTON
        val startBtn = Button(mContext)
        startBtn.text = "INITIALIZE AUTO-PILOT"
        startBtn.setBackgroundColor(Color.parseColor("#00FF00"))
        startBtn.setTextColor(Color.BLACK)
        startBtn.setTypeface(null, android.graphics.Typeface.BOLD)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 80, 0, 0)
        startBtn.layoutParams = params
        layout.addView(startBtn)

        // ⚙️ SETTINGS BUTTON
        val accBtn = Button(mContext)
        accBtn.text = "ENABLE DEC SERVICE"
        accBtn.setBackgroundColor(Color.parseColor("#333333"))
        accBtn.setTextColor(Color.WHITE)
        val accParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        accParams.setMargins(0, 30, 0, 0)
        accBtn.layoutParams = accParams
        layout.addView(accBtn)

        setContentView(layout)

        // 🖱️ BUTTON ACTIONS
        accBtn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            Toast.makeText(mContext, "Turn ON Dec Omni-Engine", Toast.LENGTH_LONG).show()
        }

        startBtn.setOnClickListener {
            val topic = topicInput.text.toString()
            if (topic.isNotBlank()) {
                // Save the topic so the Background Service can use it!
                val prefs = getSharedPreferences("DecPrefs", Context.MODE_PRIVATE)
                prefs.edit().putString("custom_topic", topic).apply()
                
                Toast.makeText(mContext, "🔥 TOPIC LOCKED! Open Claude to begin.", Toast.LENGTH_LONG).show()
                
                // Automatically open Claude App
                try {
                    val launchIntent = packageManager.getLaunchIntentForPackage("com.anthropic.claude")
                    if (launchIntent != null) {
                        startActivity(launchIntent)
                    } else {
                        Toast.makeText(mContext, "Claude app not found! Open it manually.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(mContext, "Open Claude manually.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(mContext, "❌ Please enter a topic first!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
