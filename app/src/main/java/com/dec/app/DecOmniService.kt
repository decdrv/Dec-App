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
                    
                    automationStep = 2
                }
            }
            
            // STEP 2: Smart Send Button Search
            else if (automationStep == 2) {
                // 1.5 सेकंड का इंतज़ार ताकि Send बटन Active हो जाए
                Handler(Looper.getMainLooper()).postDelayed({
                    val currentRoot = rootInActiveWindow
                    if (currentRoot != null) {
                        var clicked = false
                        
                        // Strategy A: Typing Box के बगल वाला बटन ढूंढो
                        val textBoxes = findNodesByClass(currentRoot, "android.widget.EditText")
                        if (textBoxes.isNotEmpty()) {
                            val sendBtn = findSiblingButton(textBoxes[0])
                            if (sendBtn != null) {
                                sendBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                clicked = true
                            }
                        }
                        
                        // Strategy B: अगर बगल में नहीं मिला, तो नाम से ढूंढो
                        if (!clicked) {
                            val fallbackBtn = findSendButtonByText(currentRoot)
                            if (fallbackBtn != null) {
                                fallbackBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                clicked = true
                            }
                        }
                        
                        if (clicked) {
                            Toast.makeText(this, "Dec: Clicked SEND!", Toast.LENGTH_SHORT).show()
                            automationStep = 0
                            isAutomating = false
                        } else {
                            Toast.makeText(this, "Dec: Send button is hiding!", Toast.LENGTH_LONG).show()
                            // अगर नहीं मिला, तो वापस Step 2 पर ही रहो ताकि दोबारा कोशिश कर सके
                        }
                    }
                }, 1500) 
            }
        }
    }

    // Helper 1: Class के नाम से ढूंढना
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

    // Helper 2: Smart Sibling Search (Typing Box के बगल वाला बटन)
    private fun findSiblingButton(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var parent = node.parent
        var depth = 0
        // 3 level ऊपर तक जाकर चेक करो
        while (parent != null && depth < 3) {
            for (i in 0 until parent.childCount) {
                val child = parent.getChild(i)
                // अगर कोई चीज़ Clickable है, और वह Typing Box नहीं है
                if (child != null && child.isClickable && child != node) {
                    val className = child.className?.toString() ?: ""
                    // अगर वह Image या Button है, तो 99% वही Send बटन है!
                    if (className.contains("Image") || className.contains("Button")) {
                        return child
                    }
                }
            }
            parent = parent.parent
            depth++
        }
        return null
    }

    // Helper 3: Fallback Search
    private fun findSendButtonByText(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isClickable) {
            val desc = root.contentDescription?.toString()?.lowercase() ?: ""
            if (desc.contains("send") || desc.contains("submit") || desc.contains("message")) {
                return root
            }
        }
        for (i in 0 until root.childCount) {
            val child = root.getChild(i)
            if (child != null) {
                val found = findSendButtonByText(child)
                if (found != null) return found
            }
        }
        return null
    }

    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val currentTime = System.currentTimeMillis()

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
