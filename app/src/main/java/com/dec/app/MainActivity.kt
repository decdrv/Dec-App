package com.dec.app

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
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🎨 HACKER THEME UI (Programmatic Layout)
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 100, 50, 50)
        layout.setBackgroundColor(Color.parseColor("#0D0D0D")) // Deep Black
        layout.gravity = Gravity.CENTER_HORIZONTAL

        // 🟢 TITLE
        val title = TextView(this)
        title.text = "DEC OMNI-ENGINE"
        title.textSize = 28f
        title.setTextColor(Color.parseColor("#00FF00")) // Hacker Green
        title.gravity = Gravity.CENTER
        title.setTypeface(null, android.graphics.Typeface.BOLD)
        layout.addView(title)

        // ⚪ SUBTITLE
        val subtitle = TextView(this)
        subtitle.text = "Autonomous AI Parasite\nStatus: ONLINE"
        subtitle.textSize = 14f
        subtitle.setTextColor(Color.LTGRAY)
        subtitle.gravity = Gravity.CENTER
        subtitle.setPadding(0, 10, 0, 80)
        layout.addView(subtitle)

        // 📝 TOPIC INPUT BOX
        val topicInput = EditText(this)
        topicInput.hint = "Enter Topic (e.g. Quantum Physics, Hacking)"
        topicInput.setHintTextColor(Color.DKGRAY)
        topicInput.setTextColor(Color.WHITE)
        topicInput.setBackgroundColor(Color.parseColor("#1A1A1A"))
        topicInput.setPadding(40, 40, 40, 40)
        layout.addView(topicInput)

        // 🚀 START BUTTON
        val startBtn = Button(this)
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
        val accBtn = Button(this)
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

        // 🖱️ BUTTON ACTIONS (Using applicationContext to prevent compiler confusion)
        accBtn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            Toast.makeText(applicationContext, "Turn ON Dec Omni-Engine", Toast.LENGTH_LONG).show()
        }

        startBtn.setOnClickListener {
            val topic = topicInput.text.toString()
            if (topic.isNotBlank()) {
                // Save the topic so the Background Service can use it!
                val prefs = getSharedPreferences("DecPrefs", Context.MODE_PRIVATE)
                prefs.edit().putString("custom_topic", topic).apply()
                
                Toast.makeText(applicationContext, "🔥 TOPIC LOCKED! Open Claude to begin.", Toast.LENGTH_LONG).show()
                
                // Automatically open Claude App
                try {
                    val launchIntent = packageManager.getLaunchIntentForPackage("com.anthropic.claude")
                    if (launchIntent != null) {
                        startActivity(launchIntent)
                    } else {
                        Toast.makeText(applicationContext, "Claude app not found! Open it manually.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(applicationContext, "Open Claude manually.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(applicationContext, "❌ Please enter a topic first!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
