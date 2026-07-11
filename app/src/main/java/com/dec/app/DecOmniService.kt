package com.dec.app

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.KeyEvent
import android.widget.Toast

class DecOmniService : AccessibilityService() {
    
    private var volumeDownCount = 0
    private var lastVolumeDownTime: Long = 0

    // 1. THE GOD EYE: यह function screen पर होने वाली हर हलचल को देखेगा
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // (हम यहाँ Claude को hijack करने का code बाद में डालेंगे)
    }

    override fun onInterrupt() {
        // Android system के लिए ज़रूरी
    }

    // 2. THE HARDWARE KILL SWITCH: Volume Down बटन को 3 बार दबाने पर Emergency Stop
    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && event.action == KeyEvent.ACTION_DOWN) {
            val currentTime = System.currentTimeMillis()
            
            // अगर 1 सेकंड के अंदर बटन दबाया गया है, तो count बढ़ाओ
            if (currentTime - lastVolumeDownTime < 1000) {
                volumeDownCount++
            } else {
                volumeDownCount = 1
            }
            lastVolumeDownTime = currentTime

            // अगर 3 बार दब गया, तो Dec को रोक दो!
            if (volumeDownCount >= 3) {
                Toast.makeText(this, "EMERGENCY STOP: Dec Omni-Engine Paused!", Toast.LENGTH_LONG).show()
                volumeDownCount = 0
                return true // यह बटन को असल में आवाज़ कम करने से रोक देगा
            }
        }
        return super.onKeyEvent(event)
    }

    // 3. STARTUP: जैसे ही आप Settings से इसे On करेंगे, यह जाग जाएगा
    override fun onServiceConnected() {
        super.onServiceConnected()
        Toast.makeText(this, "Dec Omni-Engine is now ONLINE and watching.", Toast.LENGTH_LONG).show()
    }
}
