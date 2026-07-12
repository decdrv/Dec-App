package com.dec.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import android.app.SearchManager

/**
 * DecOmniService: The core automation engine of Dec.
 * Handles screen reading, automated typing, web searching, and cross-app navigation.
 */
class DecOmniService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var isProcessing = false
    private var autoPilotActive = false
    private var lastSavedText = ""
    private var currentThought = ""
    private var isFirstThought = true
    
    private val brain: DecBrain by lazy { DecBrain(this) }
    private var currentState = "CHATTING" // CHATTING, SEARCHING_WEB

    override fun onServiceConnected() {
        super.onServiceConnected()
        Toast.makeText(this, "🌐 DEC OMNI-ENGINE: GOD-MODE ONLINE", Toast.LENGTH_LONG).show()
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        // Emergency Stop: Volume Down
        if (event != null && event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && event.action == KeyEvent.ACTION_DOWN) {
            autoPilotActive = false
            isProcessing = false
            currentState = "CHATTING"
            Toast.makeText(this, "🛑 EMERGENCY STOP!", Toast.LENGTH_LONG).show()
            return true
        }
        return super.onKeyEvent(event)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Activation Trigger: Type "..dec"
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            val node = event.source ?: return
            val text = node.text?.toString() ?: ""
            if (text.trim().lowercase() == "..dec") {
                startAutonomousMission(node)
            }
        }
    }

    override fun onInterrupt() {}

    private fun startAutonomousMission(textBox: AccessibilityNodeInfo) {
        isProcessing = true
        autoPilotActive = true
        
        if (isFirstThought) {
            val prefs = getSharedPreferences("DecPrefs", Context.MODE_PRIVATE)
            val topic = prefs.getString("custom_topic", "General Knowledge") ?: "General Knowledge"
            
            currentThought = """
                You are DEC, an autonomous AI agent with full system access.
                Mission: Master the topic "$topic".
                
                Formats:
                [MEMORY: <summary>] - To save info.
                [NEXT_PROMPT: <question>] - To continue learning.
                [ACTION: SEARCH_WEB: <query>] - To get live internet data.
                [ACTION: OPEN_APP: <package_name>] - To switch apps.
            """.trimIndent()
            isFirstThought = false
        }
        
        Toast.makeText(this, "⚡ MISSION STARTED", Toast.LENGTH_SHORT).show()
        executeChatLoop(textBox)
    }

    private fun executeChatLoop(textBox: AccessibilityNodeInfo) {
        if (!autoPilotActive) return

        val arguments = Bundle()
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, currentThought)
        textBox.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        
        handler.postDelayed({
            if (clickSendButton(textBox)) {
                waitForResponse()
            }
        }, 1000)
    }

    private fun clickSendButton(textBox: AccessibilityNodeInfo): Boolean {
        val root = rootInActiveWindow ?: return false
        
        // Strategy 1: Find by keyword
        val keywords = listOf("send", "submit", "arrow")
        for (key in keywords) {
            val node = findNodeByKeyword(root, key)
            if (node != null && node.isClickable) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
        }

        // Strategy 2: Sibling click (common in many chat apps)
        var parent = textBox.parent
        for (i in 0..2) {
            if (parent == null) break
            for (j in 0 until parent.childCount) {
                val sibling = parent.getChild(j)
                if (sibling != null && sibling != textBox && sibling.isClickable) {
                    sibling.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return true
                }
            }
            parent = parent.parent
        }
        return false
    }

    private fun waitForResponse() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!autoPilotActive) return
                val stopBtn = findNodeByKeyword(rootInActiveWindow, "stop responding")
                if (stopBtn != null) {
                    // Still thinking...
                    handler.postDelayed(this, 1000)
                } else {
                    // Done! Copy the response
                    copyAndProcessResponse()
                }
            }
        }, 2000)
    }

    private fun copyAndProcessResponse() {
        val copyBtn = findLastNodeByKeyword(rootInActiveWindow, "copy")
        if (copyBtn != null) {
            copyBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        handler.postDelayed({
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
            
            if (text.isNotBlank() && text != lastSavedText) {
                lastSavedText = text
                val result = brain.processKnowledge(text)
                handleBrainResult(result)
            } else {
                // Fallback: Read screen if clipboard failed
                val screenText = scrapeScreen(rootInActiveWindow)
                val result = brain.processKnowledge(screenText)
                handleBrainResult(result)
            }
        }, 1000)
    }

    private fun handleBrainResult(result: DecBrain.BrainResult) {
        when (result) {
            is DecBrain.BrainResult.NextThought -> {
                currentThought = result.prompt
                handler.postDelayed({ findTextBoxAndResume() }, 3000)
            }
            is DecBrain.BrainResult.SearchWeb -> {
                performWebSearch(result.query)
            }
            is DecBrain.BrainResult.OpenApp -> {
                launchApp(result.appName)
            }
            is DecBrain.BrainResult.ContinueThinking -> {
                currentThought = result.fallbackPrompt
                handler.postDelayed({ findTextBoxAndResume() }, 3000)
            }
        }
    }

    private fun performWebSearch(query: String) {
        currentState = "SEARCHING_WEB"
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        
        handler.postDelayed({
            performScrollDown()
            handler.postDelayed({
                val webData = scrapeScreen(rootInActiveWindow)
                performGlobalAction(GLOBAL_ACTION_BACK)
                currentThought = "I searched for '$query'. Found: \n${webData.take(1000)}"
                currentState = "CHATTING"
                handler.postDelayed({ findTextBoxAndResume() }, 3000)
            }, 3000)
        }, 5000)
    }

    private fun launchApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            startActivity(intent)
            Toast.makeText(this, "Opening $packageName", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performScrollDown() {
        val path = Path().apply {
            moveTo(500f, 1500f)
            lineTo(500f, 500f)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 500))
            .build()
        dispatchGesture(gesture, null, null)
    }

    private fun findTextBoxAndResume() {
        val textBox = findNodeByClass(rootInActiveWindow, "android.widget.EditText")
        if (textBox != null) {
            executeChatLoop(textBox)
        }
    }

    // Helper functions for node traversal
    private fun findNodeByKeyword(root: AccessibilityNodeInfo?, keyword: String): AccessibilityNodeInfo? {
        if (root == null) return null
        if (root.contentDescription?.toString()?.lowercase()?.contains(keyword) == true ||
            root.text?.toString()?.lowercase()?.contains(keyword) == true) return root
        for (i in 0 until root.childCount) {
            val res = findNodeByKeyword(root.getChild(i), keyword)
            if (res != null) return res
        }
        return null
    }

    private fun findLastNodeByKeyword(root: AccessibilityNodeInfo?, keyword: String): AccessibilityNodeInfo? {
        if (root == null) return null
        val matches = mutableListOf<AccessibilityNodeInfo>()
        fun search(node: AccessibilityNodeInfo?) {
            if (node == null) return
            if (node.contentDescription?.toString()?.lowercase()?.contains(keyword) == true ||
                node.text?.toString()?.lowercase()?.contains(keyword) == true) matches.add(node)
            for (i in 0 until node.childCount) search(node.getChild(i))
        }
        search(root)
        return matches.lastOrNull()
    }

    private fun findNodeByClass(root: AccessibilityNodeInfo?, className: String): AccessibilityNodeInfo? {
        if (root == null) return null
        if (root.className?.toString() == className) return root
        for (i in 0 until root.childCount) {
            val res = findNodeByClass(root.getChild(i), className)
            if (res != null) return res
        }
        return null
    }

    private fun scrapeScreen(root: AccessibilityNodeInfo?): String {
        if (root == null) return ""
        val sb = StringBuilder()
        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null) return
            val text = node.text?.toString()
            if (!text.isNullOrBlank() && text.length > 20) sb.append(text).append("\n")
            for (i in 0 until node.childCount) traverse(node.getChild(i))
        }
        traverse(root)
        return sb.toString()
    }
}
