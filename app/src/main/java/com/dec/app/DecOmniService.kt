package com.dec.app

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.KeyEvent
import android.widget.Toast
import android.os.Bundle
import android.os.Handler
import android.os.Looper

// FIXED: Class name now exactly matches your file name!
class DecOmniService : AccessibilityService() {

    private var isEngineActive = true
    private var handler: Handler? = null

    override fun onCreate() {
        super.onCreate()
        handler = Handler(Looper.getMainLooper())
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        showToast("Dec Omni-Engine is ONLINE")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Empty for now
    }

    override fun onInterrupt() {
        showToast("Dec Engine Interrupted!")
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        try {
            if (event == null) return false
            if (!isEngineActive) return super.onKeyEvent(event)

            val action = event.action
            val keyCode = event.keyCode

            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && action == KeyEvent.ACTION_DOWN) {
                isEngineActive = false
                showToast("🚨 EMERGENCY STOP: Dec is sleeping.")
                return true
            }

            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && action == KeyEvent.ACTION_DOWN) {
                showToast("Dec: Starting Perfect Chat Loop...")
                startPerfectChatLoop()
                return true
            }

            return super.onKeyEvent(event)
        } catch (e: Exception) {
            return false
        }
    }

    private fun startPerfectChatLoop() {
        try {
            val root = rootInActiveWindow
            if (root == null) {
                showToast("Dec: Screen not ready!")
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
                showToast("Dec: Typed message.")
            } else {
                showToast("Dec: Text box not found!")
                return
            }

            handler?.postDelayed({
                try {
                    val currentRoot = rootInActiveWindow
                    val sendBtn = findNodeByDesc(currentRoot, "Send")
                    if (sendBtn != null) {
                        sendBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        showToast("Dec: Pressed Send.")
                        pollForStopRespondingToAppear(0)
                    } else {
                        showToast("Dec: Send button not found!")
                    }
                } catch (e: Exception) {}
            }, 1000)
        } catch (e: Exception) {}
    }

    private fun pollForStopRespondingToAppear(attempts: Int) {
        try {
            if (attempts > 10) {
                pollForStopRespondingToDisappear()
                return
            }
            val stopBtn = findNodeByDesc(rootInActiveWindow, "Stop responding")
            if (stopBtn != null) {
                showToast("Dec: Smart Wait (Claude is typing...)")
                pollForStopRespondingToDisappear()
            } else {
                handler?.postDelayed({ pollForStopRespondingToAppear(attempts + 1) }, 500)
            }
        } catch (e: Exception) {}
    }

    private fun pollForStopRespondingToDisappear() {
        try {
            val stopBtn = findNodeByDesc(rootInActiveWindow, "Stop responding")
            if (stopBtn != null) {
                handler?.postDelayed({ pollForStopRespondingToDisappear() }, 500)
            } else {
                handler?.postDelayed({
                    try {
                        showToast("Dec: Response complete! Stealing data...")
                        val copyBtn = findLastNodeByDesc(rootInActiveWindow, "Copy message")
                        if (copyBtn != null) {
                            copyBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            showToast("🧠 DEC LEARNED: Data Copied!")
                        } else {
                            showToast("Dec: Copy button not found!")
                        }
                    } catch (e: Exception) {}
                }, 1000)
            }
        } catch (e: Exception) {}
    }

    private fun findNodeByDesc(root: AccessibilityNodeInfo?, desc: String): AccessibilityNodeInfo? {
        if (root == null) return null
        try {
            if (root.contentDescription?.toString() == desc) return root
            for (i in 0 until root.childCount) {
                val result = findNodeByDesc(root.getChild(i), desc)
                if (result != null) return result
            }
        } catch (e: Exception) {}
        return null
    }

    private fun findLastNodeByDesc(root: AccessibilityNodeInfo?, desc: String): AccessibilityNodeInfo? {
        if (root == null) return null
        val matches = mutableListOf<AccessibilityNodeInfo>()
        fun search(node: AccessibilityNodeInfo?) {
            if (node == null) return
            try {
                if (node.contentDescription?.toString() == desc) matches.add(node)
                for (i in 0 until node.childCount) search(node.getChild(i))
            } catch (e: Exception) {}
        }
        search(root)
        return matches.lastOrNull()
    }

    private fun findNodeByClass(root: AccessibilityNodeInfo?, className: String): AccessibilityNodeInfo? {
        if (root == null) return null
        try {
            if (root.className?.toString() == className) return root
            for (i in 0 until root.childCount) {
                val result = findNodeByClass(root.getChild(i), className)
                if (result != null) return result
            }
        } catch (e: Exception) {}
        return null
    }

    private fun showToast(message: String) {
        handler?.post {
            try {
                Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {}
        }
    }
}
