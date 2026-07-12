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
    private var isProcessing = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        Toast.makeText(this, "🟢 GHOST PROTOCOL ONLINE (Type ..dec)", Toast.LENGTH_LONG).show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || isProcessing) return

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            val node = event.source ?: return
            val text = node.text?.toString() ?: ""

            if (text.trim().lowercase() == "..dec") {
                isProcessing = true
                Toast.makeText(this, "⚡ DEC AWAKENED!", Toast.LENGTH_SHORT).show()
                startPerfectChatLoop(node)
            }
        }
    }

    override fun onInterrupt() {}

    private fun startPerfectChatLoop(textBox: AccessibilityNodeInfo) {
        val arguments = Bundle()
        arguments.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, 
            "Give me a 1-line technology fact."
        )
        textBox.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        
        handler.postDelayed({
            val clicked = tryClickingSend(textBox)
            if (clicked) {
                Toast.makeText(this, "✅ Pressed Send (Smart Click)", Toast.LENGTH_SHORT).show()
                pollForStopRespondingToAppear(0)
            } else {
                Toast.makeText(this, "❌ Send button hidden!", Toast.LENGTH_SHORT).show()
                isProcessing = false // Reset so you can try again
            }
        }, 1000)
    }

    // ⚡ THE SMART CLICKER: Bypasses Claude & ChatGPT Security
    private fun tryClickingSend(textBox: AccessibilityNodeInfo): Boolean {
        val root = rootInActiveWindow ?: return false
        
        // Method 1: Search by Keyword (Handles "Send message", "send", etc.)
        val sendBtn = findNodeByKeyword(root, "send")
        if (sendBtn != null) {
            if (sendBtn.isClickable) {
                sendBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            } else if (sendBtn.parent?.isClickable == true) {
                sendBtn.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
        }

        // Method 2: Structural Hack (Finds the clickable button next to the text box)
        var parent = textBox.parent
        for (i in 0..2) { // Look up to 3 levels deep in the container
            if (parent == null) break
            for (j in 0 until parent.childCount) {
                val sibling = parent.getChild(j)
                if (sibling != null && sibling != textBox) {
                    if (sibling.isClickable) {
                        sibling.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        return true
                    }
                }
            }
            parent = parent.parent
        }
        
        return false
    }

    // 🔍 UPGRADED SCANNER: Case-insensitive and partial matches
    private fun findNodeByKeyword(root: AccessibilityNodeInfo?, keyword: String): AccessibilityNodeInfo? {
        if (root == null) return null
        val desc = root.contentDescription?.toString()?.lowercase() ?: ""
        val text = root.text?.toString()?.lowercase() ?: ""
        
        if (desc.contains(keyword) || text.contains(keyword)) return root
        
        for (i in 0 until root.childCount) {
            val result = findNodeByKeyword(root.getChild(i), keyword)
            if (result != null) return result
        }
        return null
    }

    private fun pollForStopRespondingToAppear(attempts: Int) {
        if (attempts > 10) {
            pollForStopRespondingToDisappear()
            return
        }
        val stopBtn = findNodeByKeyword(rootInActiveWindow, "stop responding")
        if (stopBtn != null) {
            pollForStopRespondingToDisappear()
        } else {
            handler.postDelayed({ pollForStopRespondingToAppear(attempts + 1) }, 500)
        }
    }

    private fun pollForStopRespondingToDisappear() {
        val stopBtn = findNodeByKeyword(rootInActiveWindow, "stop responding")
        if (stopBtn != null) {
            handler.postDelayed({ pollForStopRespondingToDisappear() }, 500)
        } else {
            handler.postDelayed({
                val copyBtn = findLastNodeByKeyword(rootInActiveWindow, "copy")
                if (copyBtn != null) {
                    if (copyBtn.isClickable) {
                        copyBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    } else if (copyBtn.parent?.isClickable == true) {
                        copyBtn.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }
                    Toast.makeText(this, "🧠 DATA COPIED!", Toast.LENGTH_LONG).show()
                }
                isProcessing = false
            }, 1000)
        }
    }

    private fun findLastNodeByKeyword(root: AccessibilityNodeInfo?, keyword: String): AccessibilityNodeInfo? {
        if (root == null) return null
        val matches = mutableListOf<AccessibilityNodeInfo>()
        fun search(node: AccessibilityNodeInfo?) {
            if (node == null) return
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            val text = node.text?.toString()?.lowercase() ?: ""
            if (desc.contains(keyword) || text.contains(keyword)) matches.add(node)
            for (i in 0 until node.childCount) search(node.getChild(i))
        }
        search(root)
        return matches.lastOrNull()
    }
}
