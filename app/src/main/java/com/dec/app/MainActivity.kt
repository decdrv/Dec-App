package com.dec.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.util.Log
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityManager

/**
 * MainActivity: The Control Panel for the Dec Omni-Engine.
 * Features a Hacker-themed UI for initializing the autonomous agent.
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate: Activity created.")

        val mContext = this
        
        // Check Accessibility Service status on app launch
        checkAccessibilityServiceStatus()
        
        // 🎨 HACKER THEME UI
        val layout = LinearLayout(mContext).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 120, 60, 60)
            setBackgroundColor(Color.parseColor("#0D0D0D")) // Deep Black
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // 🟢 TITLE
        val title = TextView(mContext).apply {
            text = "DEC OMNI-ENGINE"
            textSize = 32f
            setTextColor(Color.parseColor("#00FF00")) // Hacker Green
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
        }
        layout.addView(title)

        // ⚪ SUBTITLE
        val subtitle = TextView(mContext).apply {
            text = "Autonomous AI Parasite\nVersion 2.0: GOD-MODE"
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 100)
        }
        layout.addView(subtitle)

        // 📝 TOPIC INPUT BOX
        val topicInput = EditText(mContext).apply {
            hint = "Enter Target Knowledge Topic..."
            setHintTextColor(Color.DKGRAY)
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1A1A1A"))
            setPadding(40, 40, 40, 40)
        }
        layout.addView(topicInput)

        // 🚀 INITIALIZE BUTTON
        val startBtn = Button(mContext).apply {
            text = "INITIALIZE AGENT"
            setBackgroundColor(Color.parseColor("#00FF00"))
            setTextColor(Color.BLACK)
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 80, 0, 0) }
        }
        layout.addView(startBtn)

        // ⚙️ ACCESSIBILITY BUTTON
        val accBtn = Button(mContext).apply {
            text = "ENABLE CORE SERVICE"
            setBackgroundColor(Color.parseColor("#333333"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 30, 0, 0) }
        }
        layout.addView(accBtn)

        setContentView(layout)

        // 🖱️ ACTIONS
        accBtn.setOnClickListener {
            Log.d("MainActivity", "ENABLE CORE SERVICE button clicked.")
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            showMsg("Enable 'Dec Omni-Engine' in Accessibility Settings")
        }

        startBtn.setOnClickListener {
            Log.d("MainActivity", "INITIALIZE AGENT button clicked.")
            val topic = topicInput.text.toString()
            if (topic.isNotBlank()) {
                val prefs = getSharedPreferences("DecPrefs", Context.MODE_PRIVATE)
                prefs.edit().putString("custom_topic", topic).apply()
                
                showMsg("🔥 MISSION LOCKED: $topic")
                
                // Try to launch Claude
                try {
                    val launchIntent = packageManager.getLaunchIntentForPackage("com.anthropic.claude")
                    if (launchIntent != null) {
                        startActivity(launchIntent)
                        Log.i("MainActivity", "Attempting to launch Claude app.")
                    } else {
                        showMsg("Claude app not found. Launching Browser...")
                        val browserIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://claude.ai"))
                        startActivity(browserIntent)
                        Log.w("MainActivity", "Claude app not found, launching browser to claude.ai.")
                    }
                } catch (e: Exception) {
                    showMsg("Launch failed. Open your AI app manually.")
                    Log.e("MainActivity", "Failed to launch Claude or browser: ${e.message}")
                }
            } else {
                showMsg("❌ Mission requires a Topic!")
                Log.w("MainActivity", "Mission initialization failed: Topic is blank.")
            }
        }
    }

    private fun showMsg(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.d("MainActivity", "Toast message shown: $message")
    }

    private fun checkAccessibilityServiceStatus() {
        val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        var isServiceEnabled = false
        for (service in enabledServices) {
            if (service.resolveInfo.serviceInfo.packageName == packageName &&
                service.resolveInfo.serviceInfo.name == DecOmniService::class.java.name) {
                isServiceEnabled = true
                break
            }
        }

        if (!isServiceEnabled) {
            showMsg("⚠️ DEC Omni-Engine Accessibility Service is NOT enabled. Please enable it in settings.")
            Log.w("MainActivity", "Accessibility Service is not enabled.")
            // Optionally, automatically open settings
            // startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } else {
            Log.i("MainActivity", "Accessibility Service is enabled.")
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume: Activity resumed.")
        checkAccessibilityServiceStatus()
    }
}
