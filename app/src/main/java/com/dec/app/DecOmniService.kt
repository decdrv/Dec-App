package com.dec.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
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
                    
                    if (!textBox.isFocused) {
                        textBox.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }

                    val arguments = Bundle()
                    arguments.putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, 
                        "Give me a very short, 1-line technology fact."
                    )
                    textBox.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                    
                    Toast.makeText(this, "Dec: Typed! Waiting for UI...", Toast.LENGTH_SHORT).show()
                    automationStep = 2 
                    
                    // STEP 2: 1.5 सेकंड रुको (ताकि Keyboard पूरी तरह खुल जाए) और फिर Sniper Shot मारो!
                    Handler(Looper.getMainLooper()).postDelayed({
                        fireSniperShot()
                    }, 1500)
                }
            }
        }
    }

    // THE PERMANENT FIX: Physical Screen Tap (Gesture Dispatch)
    private fun fireSniperShot() {
        val currentRoot = rootInActiveWindow
        if (currentRoot != null) {
            val sendBtn = findNodeByDescription(currentRoot, "Send")
            
            if (sendBtn != null) {
                // बटन की असली X, Y Location निकालो
                val rect = Rect()
                sendBtn.getBoundsInScreen(rect)
                
                // Screen पर बिल्कुल बीचों-बीच (Center) निशाना लगाओ
                val x = rect.centerX().toFloat()
                val y = rect.centerY().toFloat()
                
                val path = Path()
                path.moveTo(x, y)
                
                // असली उंगली की तरह Tap करो (100 milliseconds का प्रहार)
                val gesture = GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
                    .build()
                    
                dispatchGesture(gesture, null, null)
                Toast.makeText(this, "Dec: SNIPER SHOT FIRED! 🎯", Toast.LENGTH_LONG).show()
                
                automationStep = 0
                isAutomating = false
            } else {
                Toast.makeText(this, "Dec: Send button not found!", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
