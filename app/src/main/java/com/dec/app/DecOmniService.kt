package com.dec.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.KeyEvent
import android.widget.Toast

class DecOmniService : AccessibilityService() {
    
    private var volumeDownCount = 0
    private var lastVolumeDownTime: Long = 0
    
    private var volumeUpCount = 0
    private var lastVolumeUpTime: Long = 0

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // (हम यहाँ Claude के अंदर type करने और पढ़ने का code अगले step में डालेंगे)
    }

    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val currentTime = System.currentTimeMillis()

        // 1. THE KILL SWITCH: Volume Down (3 बार)
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && event.action == KeyEvent.ACTION_DOWN) {
            if (currentTime - lastVolumeDownTime < 1000) volumeDownCount++ else volumeDownCount = 1
            lastVolumeDownTime = currentTime

            if (volumeDownCount >= 3) {
                Toast.makeText(this, "EMERGENCY STOP: Dec Omni-Engine Paused!", Toast.LENGTH_LONG).show()
                volumeDownCount = 0
                return true
            }
        }

        // 2. THE CLAUDE HIJACKER: Volume Up (2 बार)
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP && event.action == KeyEvent.ACTION_DOWN) {
            if (currentTime - lastVolumeUpTime < 1000) volumeUpCount++ else volumeUpCount = 1
            lastVolumeUpTime = currentTime

            if (volumeUpCount >= 2) {
                Toast.makeText(this, "Dec: Hijacking Claude Brain...", Toast.LENGTH_SHORT).show()
                hijackClaude()
                volumeUpCount = 0
                return true
            }
        }

        return super.onKeyEvent(event)
    }

    // यह Function Claude App को background से खींचकर सामने लाएगा
    private fun hijackClaude() {
        // Claude app का official package name
        val intent = packageManager.getLaunchIntentForPackage("com.anthropic.claude")
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } else {
            Toast.makeText(this, "Boss, Claude App is not installed!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Toast.makeText(this, "Dec Omni-Engine is ONLINE.", Toast.LENGTH_LONG).show()
    }
}
