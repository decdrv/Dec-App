package com.dec.app

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.KeyEvent
import android.widget.Toast

class DecOmniService : AccessibilityService() {
    
    private var volumeUpCount = 0
    private var lastVolumeUpTime: Long = 0

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val currentTime = System.currentTimeMillis()

        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP && event.action == KeyEvent.ACTION_DOWN) {
            if (currentTime - lastVolumeUpTime < 1000) volumeUpCount++ else volumeUpCount = 1
            lastVolumeUpTime = currentTime
            
            if (volumeUpCount >= 2) {
                Toast.makeText(this, "Dec: Scanning Screen...", Toast.LENGTH_SHORT).show()
                scanAndCopyToClipboard()
                volumeUpCount = 0
                return true
            }
        }
        return super.onKeyEvent(event)
    }

    private fun scanAndCopyToClipboard() {
        val rootNode = rootInActiveWindow
        if (rootNode == null) {
            Toast.makeText(this, "Screen is empty!", Toast.LENGTH_SHORT).show()
            return
        }

        val sb = java.lang.StringBuilder()
        sb.append("--- DEC X-RAY VISION (CLAUDE APP) ---\n")
        dumpNode(rootNode, 0, sb)

        // Copy to Phone's Clipboard
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Dec UI Dump", sb.toString())
        clipboard.setPrimaryClip(clip)

        Toast.makeText(this, "Screen Copied! Paste it to Boss.", Toast.LENGTH_LONG).show()
    }

    private fun dumpNode(node: AccessibilityNodeInfo?, depth: Int, sb: java.lang.StringBuilder) {
        if (node == null) return
        
        // सिर्फ वो चीज़ें निकालो जो Clickable हैं या जिनमें Text है
        if (node.isClickable || node.text != null || node.contentDescription != null) {
            val indent = " ".repeat(depth * 2)
            sb.append("$indent[${node.className}] ")
            if (node.text != null) sb.append("Text: '${node.text}' ")
            if (node.contentDescription != null) sb.append("Desc: '${node.contentDescription}' ")
            if (node.isClickable) sb.append("(CLICKABLE)")
            sb.append("\n")
        }

        for (i in 0 until node.childCount) {
            dumpNode(node.getChild(i), depth + 1, sb)
        }
    }
}
