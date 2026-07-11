package com.dec.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.KeyEvent
import android.widget.Toast

class DecOmniService : AccessibilityService() {
    
    private var volumeDownCount = 0
    private var lastVolumeDownTime: Long = 0
    
    private var volumeUpCount = 0
    private var lastVolumeUpTime: Long = 0

    // AUTOMATION MEMORY (यह Dec को याद दिलाएगा कि उसे क्या करना है)
    private var isAutomating = false
    private var automationStep = 0

    // 1. THE GOD EYE & GHOST FINGERS
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isAutomating) return // अगर Automation Off है, तो कुछ मत करो
        
        val rootNode = rootInActiveWindow ?: return

        // अगर हम Claude App के अंदर हैं और Step 1 पर हैं
        if (event?.packageName == "com.anthropic.claude" && automationStep == 1) {
            
            // Screen पर Typing Box (EditText) ढूंढो
            val textBoxes = findNodesByClass(rootNode, "android.widget.EditText")
            
            if (textBoxes.isNotEmpty()) {
                val textBox = textBoxes[0]
                
                // Ghost Fingers: खुद type करो!
                val arguments = Bundle()
                arguments.putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, 
                    "Hello Claude, I am Dec. Teach me a random technology fact."
                )
                textBox.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                
                Toast.makeText(this, "Dec: Typing successful!", Toast.LENGTH_SHORT).show()
                
                // Step पूरा हो गया, अब रुक जाओ (ताकि बार-बार type न करे)
                automationStep = 0
                isAutomating = false 
            }
        }
    }

    // Helper Function: Screen पर specific चीज़ें ढूंढने के लिए
    private fun findNodesByClass(root: AccessibilityNodeInfo, className: String): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        if (root.className?.toString()?.contains(className) == true) {
            result.add(root)
        }
        for (i in 0 until root.childCount) {
            val child = root.getChild(i)
            if (child != null) {
                result.addAll(findNodesByClass(child, className))
            }
        }
        return result
    }

    override fun onInterrupt() {}

    // 2. THE HARDWARE CONTROLS
    override fun onKeyEvent(event: KeyEvent): Boolean {
        val currentTime = System.currentTimeMillis()

        // KILL SWITCH: Volume Down (3 बार)
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && event.action == KeyEvent.ACTION_DOWN) {
            if (currentTime - lastVolumeDownTime < 1000) volumeDownCount++ else volumeDownCount = 1
            lastVolumeDownTime = currentTime

            if (volumeDownCount >= 3) {
                isAutomating = false
                automationStep = 0
                Toast.makeText(this, "EMERGENCY STOP: Dec Omni-Engine Paused!", Toast.LENGTH_LONG).show()
                volumeDownCount = 0
                return true
            }
        }

        // START AUTOMATION: Volume Up (2 बार)
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP && event.action == KeyEvent.ACTION_DOWN) {
            if (currentTime - lastVolumeUpTime < 1000) volumeUpCount++ else volumeUpCount = 1
            lastVolumeUpTime = currentTime

            if (volumeUpCount >= 2) {
                Toast.makeText(this, "Dec: Starting Night School Protocol...", Toast.LENGTH_SHORT).show()
                
                // Automation On करो और Claude खोलो
                isAutomating = true
                automationStep = 1
                hijackClaude()
                
                volumeUpCount = 0
                return true
            }
        }

        return super.onKeyEvent(event)
    }

    private fun hijackClaude() {
        val intent = packageManager.getLaunchIntentForPackage("com.anthropic.claude")
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Toast.makeText(this, "Dec Omni-Engine is ONLINE.", Toast.LENGTH_LONG).show()
    }
}
