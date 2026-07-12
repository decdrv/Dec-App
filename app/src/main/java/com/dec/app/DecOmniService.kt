package com.dec.app

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.KeyEvent
import android.widget.Toast
import android.os.Bundle
import android.os.Handler
import android.os.Looper

class DecOmni : AccessibilityService() {

    private var isEngineActive = true
    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        showToast("Dec Omni-Engine is ONLINE (Smart Wait Active)")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We are using Hardware Buttons to trigger actions
    }

    override fun onInterrupt() {
        showToast("Dec Engine Interrupted!")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!isEngineActive) return super.onKeyEvent(event)

        val action = event.action
        val keyCode = event.keyCode

        // EMERGENCY STOP: Volume Down
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && action == KeyEvent.ACTION_DOWN) {
            isEngineActive = false
            showToast("🚨 EMERGENCY STOP: Dec is sleeping.")
            return true
        }

        // TRIGGER THE PERFECT CHAT LOOP: Volume Up
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && action == KeyEvent.ACTION_DOWN) {
            showToast("Dec: Starting Perfect Chat Loop...")
            startPerfectChatLoop()
            return true
        }

        return super.onKeyEvent(event)
    }

    // ==========================================
    // THE PERFECT CHAT LOOP (SMART WAIT ENGINE)
    // ==========================================
    private fun startPerfectChatLoop() {
        val root = rootInActiveWindow ?: return

        // STEP 1: Find Text Box and Type
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

        // STEP 2: Wait 1 second, then press Send
        handler.postDelayed({
            val currentRoot = rootInActiveWindow
            val sendBtn = findNodeByDesc(currentRoot, "Send")
            if (sendBtn != null) {
                sendBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                showToast("Dec: Pressed Send.")
                
                // STEP 3: Start Smart Wait (Polling)
                pollForStopRespondingToAppear(0)
            } else {
                showToast("Dec: Send button not found!")
            }
        }, 1000)
    }

    // SMART WAIT: Wait for Claude to start thinking
    private fun pollForStopRespondingToAppear(attempts: Int) {
        if (attempts > 10) {
            // If it doesn't appear in 5 seconds, maybe internet is fast and it's already done!
            pollForStopRespondingToDisappear()
            return
        }
        
        val stopBtn = findNodeByDesc(rootInActiveWindow, "Stop responding")
        if (stopBtn != null) {
            showToast("Dec: Smart Wait (Claude is typing...)")
            pollForStopRespondingToDisappear()
        } else {
            // Check again in 0.5 seconds
            handler.postDelayed({ pollForStopRespondingToAppear(attempts + 1) }, 500)
        }
    }

    // SMART WAIT: Wait for Claude to finish
    private fun pollForStopRespondingToDisappear() {
        val stopBtn = findNodeByDesc(rootInActiveWindow, "Stop responding")
        if (stopBtn != null) {
            // Still typing... check again in 0.5 seconds
            handler.postDelayed({ pollForStopRespondingToDisappear() }, 500)
        } else {
            // STEP 4: Finished! Steal the Data
            handler.postDelayed({
                showToast("Dec: Response complete! Stealing data...")
                // We use findLASTNode because we want the newest message, not old history!
                val copyBtn = findLastNodeByDesc(rootInActiveWindow, "Copy message")
                if (copyBtn != null) {
                    copyBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    showToast("🧠 DEC LEARNED: Data Copied to Clipboard!")
                } else {
                    showToast("Dec: Copy button not found!")
                }
            }, 1000) // 1 second buffer for UI to settle
        }
    }

    // ==========================================
    // X-RAY HELPER FUNCTIONS
    // ==========================================
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
        return matches.lastOrNull() // Returns the very last one found (the newest message)
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

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
