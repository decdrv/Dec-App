package com.dec.app

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.KeyEvent
import android.widget.Toast
import android.os.Bundle
import android.os.Handler
import android.os.Looper

class DecOmniService : AccessibilityService() {

    private var isEngineActive = true
    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        // DIRECT TOAST: No handler, no applicationContext (Bypasses restrictions)
        Toast.makeText(this, "🟢 DEC IS ONLINE & READY!", Toast.LENGTH_LONG).show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return false
        if (!isEngineActive) return super.onKeyEvent(event)

        val action = event.action
        val keyCode = event.keyCode

        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && action == KeyEvent.ACTION_DOWN) {
            isEngineActive = false
            Toast.makeText(this, "🚨 DEC SLEEPING", Toast.LENGTH_SHORT).show()
            return true
        }

        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && action == KeyEvent.ACTION_DOWN) {
            Toast.makeText(this, "⚡ DEC: Action Triggered!", Toast.LENGTH_SHORT).show()
            startPerfectChatLoop()
            return true // Stops the real volume from increasing
        }

        return super.onKeyEvent(event)
    }

    private fun startPerfectChatLoop() {
        val root = rootInActiveWindow
        if (root == null) {
            Toast.makeText(this, "❌ Screen not ready!", Toast.LENGTH_SHORT).show()
            return
        }

        val textBox = findNodeByClass(root, "android.widget.EditText")
        if (textBox != null) {
            val arguments = Bundle()
            arguments.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, 
                "Give me a 1-line technology fact."
            )
            textBox.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            Toast.makeText(this, "✅ Typed message", Toast.LENGTH_SHORT).show()
            
            handler.postDelayed({
                val currentRoot = rootInActiveWindow
                val sendBtn = findNodeByDesc(currentRoot, "Send")
                if (sendBtn != null) {
                    sendBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Toast.makeText(this, "✅ Pressed Send", Toast.LENGTH_SHORT).show()
                    pollForStopRespondingToAppear(0)
                } else {
                    Toast.makeText(this, "❌ Send button not found", Toast.LENGTH_SHORT).show()
                }
            }, 1000)
        } else {
            Toast.makeText(this, "❌ Text box not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pollForStopRespondingToAppear(attempts: Int) {
        if (attempts > 10) {
            pollForStopRespondingToDisappear()
            return
        }
        val stopBtn = findNodeByDesc(rootInActiveWindow, "Stop responding")
        if (stopBtn != null) {
            Toast.makeText(this, "⏳ Smart Wait...", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "✅ Response complete!", Toast.LENGTH_SHORT).show()
                val copyBtn = findLastNodeByDesc(rootInActiveWindow, "Copy message")
                if (copyBtn != null) {
                    copyBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Toast.makeText(this, "🧠 DATA COPIED!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "❌ Copy button not found", Toast.LENGTH_SHORT).show()
                }
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

    private fun findNodeByClass(root: AccessibilityNodeInfo?, className: String): AccessibilityNodeInfo? {
        if (root == null) return null
        if (root.className?.toString() == className) return root
        for (i in 0 until root.childCount) {
            val result = findNodeByClass(root.getChild(i), className)
            if (result != null) return result
        }
        return null
    }
}
