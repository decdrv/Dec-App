package com.dec.app

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import android.os.Bundle
import android.os.Handler
import android.os.Looper

class DecOmniService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var isProcessing = false // Prevents double-triggering

    override fun onServiceConnected() {
        super.onServiceConnected()
        Toast.makeText(this, "🟢 GHOST PROTOCOL ONLINE (Type ..dec)", Toast.LENGTH_LONG).show()
    }

    // ⚡ THE EXTREME ADVANCE TRIGGER: No Buttons, Just Screen Reading!
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || isProcessing) return

        // Listen to what is being typed on the screen
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            val node = event.source ?: return
            val text = node.text?.toString() ?: ""

            // THE MAGIC WORD: If user types "..dec"
            if (text.trim().lowercase() == "..dec") {
                isProcessing = true
                Toast.makeText(this, "⚡ DEC AWAKENED!", Toast.LENGTH_SHORT).show()
                startPerfectChatLoop(node) // Pass the text box directly!
            }
        }
    }

    override fun onInterrupt() {}

    private fun startPerfectChatLoop(textBox: AccessibilityNodeInfo) {
        // STEP 1: Overwrite "..dec" with the real command
        val arguments = Bundle()
        arguments.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, 
            "Give me a 1-line technology fact."
        )
        textBox.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        
        // STEP 2: Wait 1 second, then press Send
        handler.postDelayed({
            val sendBtn = findNodeByDesc(rootInActiveWindow, "Send")
            if (sendBtn != null) {
                sendBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                pollForStopRespondingToAppear(0)
            } else {
                isProcessing = false // Reset if failed
            }
        }, 1000)
    }

    private fun pollForStopRespondingToAppear(attempts: Int) {
        if (attempts > 10) {
            pollForStopRespondingToDisappear()
            return
        }
        val stopBtn = findNodeByDesc(rootInActiveWindow, "Stop responding")
        if (stopBtn != null) {
            pollForStopRespondingToDisappear()
        } else {
            handler.postDelayed({ pollForStopRespondingToAppear(attempts + 1) }, 500)
        }
    }

    private fun pollForStopRespondingToDisappear() {
        val stopBtn = findNodeByDesc(rootInActiveWindow, "Stop responding")
        if (stopBtn != null) {
            handler.postDelayed({ pollForStopRespondingToDisappear() }, 500)
        } else {
            handler.postDelayed({
                val copyBtn = findLastNodeByDesc(rootInActiveWindow, "Copy message")
                if (copyBtn != null) {
                    copyBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Toast.makeText(this, "🧠 DATA COPIED!", Toast.LENGTH_LONG).show()
                }
                isProcessing = false // Loop Complete, ready for next time!
            }, 1000)
        }
    }

    private fun findNodeByDesc(root: AccessibilityNodeInfo?, desc: String): AccessibilityNodeInfo? {
        if (root == null) return null
        if (root.contentDescription?.toString() == desc) return root
        for (i in 0 until root.childCount) {
            val result = findNodeByDesc(root.getChild(i), desc)
            if (result != null) return result
        }
        return null
    }

    private fun findLastNodeByDesc(root: AccessibilityNodeInfo?, desc: String): AccessibilityNodeInfo? {
        if (root == null) return null
        val matches = mutableListOf<AccessibilityNodeInfo>()
        fun search(node: AccessibilityNodeInfo?) {
            if (node == null) return
            if (node.contentDescription?.toString() == desc) matches.add(node)
            for (i in 0 until node.childCount) search(node.getChild(i))
        }
        search(root)
        return matches.lastOrNull()
    }
}
