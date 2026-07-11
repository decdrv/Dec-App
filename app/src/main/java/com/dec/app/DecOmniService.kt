package com.dec.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.KeyEvent
import android.widget.Toast
import android.os.Handler
import android.os.Looper

class DecOmniService : AccessibilityService() {
    
    private var volumeDownCount = 0
    private var lastVolumeDownTime: Long = 0
    private var volumeUpCount = 0
    private var lastVolumeUpTime: Long = 0

    private var isAutomating = false
    private var automationStep = 0

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isAutomating) return 
        
        val rootNode = rootInActiveWindow ?: return

        if (event?.packageName == "com.anthropic.claude") {
            
            // STEP 1: Typing Box ढूंढो और Type करो
            if (automationStep == 1) {
                val textBoxes = findNodesByClass(rootNode, "android.widget.EditText")
                if (textBoxes.isNotEmpty()) {
                    val textBox = textBoxes[0]
                    
                    // Self-Healing: अगर Box Active नहीं है, तो पहले Click करो
                    if (!textBox.isFocused) {
                        textBox.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }

                    val arguments = Bundle()
                    arguments.putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, 
                        "Give me a very short, 1-line technology fact."
                    )
                    textBox.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                    
                    Toast.makeText(this, "Dec: Typed!", Toast.LENGTH_SHORT).show()
                    automationStep = 2 // अब Send बटन ढूंढने जाओ
                }
            }
            
            // STEP 2: X-Ray Data से मिला असली Send बटन दबाओ
            else if (automationStep == 2) {
                // Dynamic Wait: 1 सेकंड रुको ताकि Send बटन Screen पर आ जाए
                Handler(Looper.getMainLooper()).postDelayed({
                    val currentRoot = rootInActiveWindow
                    if (currentRoot != null) {
                        // X-Ray Data: Desc: 'Send'
                        val sendBtn = findNodeByDescription(currentRoot, "Send")
                        
                        if (sendBtn != null && sendBtn.isClickable) {
                            sendBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            Toast.makeText(this, "Dec: CLICKED SEND! 🚀", Toast.LENGTH_LONG).show()
                            
                            // Mission Accomplished! 
                            automationStep = 0
                            isAutomating = false
                        }
                    }
                }, 1000) 
            }
        }
    }

    // Helper 1: Class के नाम से ढूंढना (Typing Box के लिए)
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

    // Helper 2: X-Ray Data वाला Description Search (Send बटन के लिए)
    private fun findNodeByDescription(root: AccessibilityNodeInfo, desc: String): AccessibilityNodeInfo? {
        if (root.contentDescription?.toString()?.equals(desc, ignoreCase = true) == true) {
            return root
        }
        for (i in 0 until root.childCount) {
            val child = root.getChild(i)
            if (child != null) {
                val found = findNodeByDescription(child, desc)
                if (found != null) return found
            }
        }
        return null
    }

    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val currentTime = System.currentTimeMillis()

        // EMERGENCY STOP (Volume Down x3)
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && event.action == KeyEvent.ACTION_DOWN) {
            if (currentTime - lastVolumeDownTime < 1000) volumeDownCount++ else volumeDownCount = 1
            lastVolumeDownTime = currentTime
            if (volumeDownCount >= 3) {
                isAutomating = false
                automationStep = 0
                Toast.makeText(this, "EMERGENCY STOP!", Toast.LENGTH_LONG).show()
                volumeDownCount = 0
                return true
            }
        }

        // START AUTOMATION (Volume Up x2)
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP && event.action == KeyEvent.ACTION_DOWN) {
            if (currentTime - lastVolumeUpTime < 1000) volumeUpCount++ else volumeUpCount = 1
            lastVolumeUpTime = currentTime
            if (volumeUpCount >= 2) {
                Toast.makeText(this, "Dec: Starting Engine...", Toast.LENGTH_SHORT).show()
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
        } else {
            Toast.makeText(this, "Claude App not found!", Toast.LENGTH_SHORT).show()
        }
    }
}
