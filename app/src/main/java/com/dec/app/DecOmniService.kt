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
                    
                    val arguments = Bundle()
                    arguments.putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, 
                        "Give me a very short, 1-line technology fact."
                    )
                    textBox.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                    
                    Toast.makeText(this, "Dec: Typed the question!", Toast.LENGTH_SHORT).show()
                    
                    // Type करने के बाद Step 2 पर जाओ
                    automationStep = 2
                }
            }
            
            // STEP 2: Send बटन ढूंढो और Click करो
            else if (automationStep == 2) {
                // थोड़ा सा इंतज़ार करो ताकि App Send बटन को screen पर दिखा सके
                Handler(Looper.getMainLooper()).postDelayed({
                    val currentRoot = rootInActiveWindow
                    if (currentRoot != null) {
                        // Send बटन ढूंढने की कोशिश (Clickable ImageViews या Buttons)
                        val sendButton = findSendButton(currentRoot)
                        
                        if (sendButton != null) {
                            sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            Toast.makeText(this, "Dec: Clicked SEND!", Toast.LENGTH_SHORT).show()
                            
                            // काम पूरा हो गया, अब रुक जाओ
                            automationStep = 0
                            isAutomating = false
                        }
                    }
                }, 1000) // 1 सेकंड का इंतज़ार
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

    // Helper 2: Send बटन ढूंढना (यह बहुत Smart तरीका है)
    private fun findSendButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Chat apps में Send बटन अक्सर एक Clickable ImageView या Button होता है
        if (root.isClickable) {
            val className = root.className?.toString() ?: ""
            val contentDesc = root.contentDescription?.toString()?.lowercase() ?: ""
            
            // अगर बटन का नाम 'send' है, या वह एक Clickable Image है (जो EditText नहीं है)
            if (contentDesc.contains("send") || 
               (className.contains("ImageView") || className.contains("Button"))) {
                return root
            }
        }
        
        for (i in 0 until root.childCount) {
            val child = root.getChild(i)
            if (child != null) {
                val found = findSendButton(child)
                if (found != null) return found
            }
        }
        return null
    }

    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val currentTime = System.currentTimeMillis()

        // KILL SWITCH: Volume Down (3 बार)
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

        // START AUTOMATION: Volume Up (2 बार)
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP && event.action == KeyEvent.ACTION_DOWN) {
            if (currentTime - lastVolumeUpTime < 1000) volumeUpCount++ else volumeUpCount = 1
            lastVolumeUpTime = currentTime
            if (volumeUpCount >= 2) {
                Toast.makeText(this, "Dec: Starting Parasite Mode...", Toast.LENGTH_SHORT).show()
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
    }
}
