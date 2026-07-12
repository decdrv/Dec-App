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
import android.util.Log
import android.app.SearchManager
import android.view.accessibility.AccessibilityManager
import android.accessibilityservice.AccessibilityServiceInfo
import android.provider.Settings
import android.net.Uri

class DecOmniService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var isProcessing = false
    private var autoPilotActive = false
    private var lastSavedText = ""
    private var currentThought = ""
    private var isFirstThought = true
    
    private val brain: DecBrain by lazy { DecBrain(this) }
    private val voice: DecVoice by lazy { DecVoice(this) }
    private var currentState = "IDLE" // IDLE, CHATTING, SEARCHING_WEB

    // New method to check if the service is enabled
    fun isServiceEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (service in enabledServices) {
            if (service.resolveInfo.serviceInfo.packageName == context.packageName &&
                service.resolveInfo.serviceInfo.name == DecOmniService::class.java.name) {
                return true
            }
        }
        return false
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("DecOmniService", "onServiceConnected: Accessibility Service connected successfully.")
        Toast.makeText(this, "🛡️ DEC OMNI-AGENT: FULL SPECTRUM ONLINE", Toast.LENGTH_LONG).show()
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event != null && event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && event.action == KeyEvent.ACTION_DOWN) {
            autoPilotActive = false
            isProcessing = false
            voice.stop()
            Toast.makeText(this, "🛑 AGENT SUSPENDED!", Toast.LENGTH_LONG).show()
            return true
        }
        return super.onKeyEvent(event)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) {
            Log.w("DecOmniService", "onAccessibilityEvent: Received null event.")
            return
        }
        Log.v("DecOmniService", "onAccessibilityEvent: Type=${event.eventType}, Package=${event.packageName}, Class=${event.className}")
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            val node = event.source ?: run {
                Log.e("DecOmniService", "onAccessibilityEvent: Event source is null for TYPE_VIEW_TEXT_CHANGED.")
                return
            }
            val text = node.text?.toString() ?: ""
            Log.d("DecOmniService", "onAccessibilityEvent: Text changed to: \"$text\"")
            if (text.trim().lowercase() == "..dec") {
                Log.i("DecOmniService", "onAccessibilityEvent: Trigger keyword \"..dec\" detected. Starting mission.")
                startMission(node)
            }
        }
    }

    override fun onInterrupt() {
        Log.d("DecOmniService", "onInterrupt: Accessibility Service interrupted.")
        voice.shutdown()
    }

    private fun startMission(textBox: AccessibilityNodeInfo) {
        Log.d("DecOmniService", "startMission: Initializing mission.")
        isProcessing = true
        autoPilotActive = true
        currentState = "CHATTING"
        
        if (isFirstThought) {
            val prefs = getSharedPreferences("DecPrefs", Context.MODE_PRIVATE)
            val topic = prefs.getString("custom_topic", "Self-Improvement") ?: "Self-Improvement"
            
            currentThought = """
                You are DEC, an autonomous AI agent.
                MISSION: Master "$topic".
                
                COMMANDS:
                [MEMORY: <summary>]
                [NEXT_PROMPT: <question>]
                [ACTION: SEARCH_WEB: <query>]
                [ACTION: OPEN_APP: <package_name>]
                [ACTION: SPEAK: <text>]
            """.trimIndent()
            isFirstThought = false
            Log.d("DecOmniService", "startMission: Initial thought generated: $currentThought")
        }
        executeChatLoop(textBox)
    }
        isProcessing = true
        autoPilotActive = true
        currentState = "CHATTING"
        
        if (isFirstThought) {
            val prefs = getSharedPreferences("DecPrefs", Context.MODE_PRIVATE)
            val topic = prefs.getString("custom_topic", "Self-Improvement") ?: "Self-Improvement"
            
            currentThought = """
                You are DEC, an autonomous AI agent.
                MISSION: Master "$topic".
                
                COMMANDS:
                [MEMORY: <summary>]
                [NEXT_PROMPT: <question>]
                [ACTION: SEARCH_WEB: <query>]
                [ACTION: OPEN_APP: <package_name>]
                [ACTION: SPEAK: <text>]
            """.trimIndent()
            isFirstThought = false
        }
        executeChatLoop(textBox)
    }

    private fun handleBrainResult(result: DecBrain.BrainResult) {
        Log.d("DecOmniService", "handleBrainResult: Processing brain result: $result")
        when (result) {
            is DecBrain.BrainResult.NextThought -> {
                currentThought = result.prompt
                Log.d("DecOmniService", "handleBrainResult: Next thought: $currentThought")
                handler.postDelayed({ findTextBoxAndResume() }, 3000)
            }
            is DecBrain.BrainResult.SearchWeb -> {
                Log.d("DecOmniService", "handleBrainResult: Performing web search for: ${result.query}")
                performWebSearch(result.query)
            }
            is DecBrain.BrainResult.OpenApp -> {
                Log.d("DecOmniService", "handleBrainResult: Opening app: ${result.appName}")
                launchApp(result.appName)
                handler.postDelayed({ findTextBoxAndResume() }, 5000)
            }
            is DecBrain.BrainResult.ContinueThinking -> {
                currentThought = result.fallbackPrompt
                Log.d("DecOmniService", "handleBrainResult: Continuing thinking with fallback prompt: $currentThought")
                handler.postDelayed({ findTextBoxAndResume() }, 3000)
            }
            is DecBrain.BrainResult.Speak -> {
                Log.d("DecOmniService", "handleBrainResult: Speaking: ${result.text}")
                voice.speak(result.text)
                handler.postDelayed({ findTextBoxAndResume() }, 3000)
            }
        }
    }
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
                handler.postDelayed({ findTextBoxAndResume() }, 5000)
            }
            is DecBrain.BrainResult.ContinueThinking -> {
                currentThought = result.fallbackPrompt
                handler.postDelayed({ findTextBoxAndResume() }, 3000)
            }
        }
    }

    private fun performWebSearch(query: String) {
        Log.d("DecOmniService", "performWebSearch: Starting web search for query: $query")
        currentState = "SEARCHING_WEB"
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        
        handler.postDelayed({
            Log.d("DecOmniService", "performWebSearch: Performing swipe to scroll down.")
            performSwipe(500f, 1500f, 500f, 500f) // Scroll Down
            handler.postDelayed({
                val webData = scrapeScreen(rootInActiveWindow)
                Log.d("DecOmniService", "performWebSearch: Scraped web data (first 200 chars): ${webData.take(200)}")
                performGlobalAction(GLOBAL_ACTION_BACK)
                currentThought = "Web Search Result for '$query':\n${webData.take(1000)}\nAnalyze and proceed."
                currentState = "CHATTING"
                Log.d("DecOmniService", "performWebSearch: Resuming chat loop after web search.")
                handler.postDelayed({ findTextBoxAndResume() }, 3000)
            }, 3000)
        }, 5000)
    }
        currentState = "SEARCHING_WEB"
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        
        handler.postDelayed({
            performSwipe(500f, 1500f, 500f, 500f) // Scroll Down
            handler.postDelayed({
                val webData = scrapeScreen(rootInActiveWindow)
                performGlobalAction(GLOBAL_ACTION_BACK)
                currentThought = "Web Search Result for '$query':\n${webData.take(1000)}\nAnalyze and proceed."
                currentState = "CHATTING"
                handler.postDelayed({ findTextBoxAndResume() }, 3000)
            }, 3000)
        }, 5000)
    }

    private fun launchApp(packageName: String) {
        Log.d("DecOmniService", "launchApp: Attempting to launch app with package name: $packageName")
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            startActivity(intent)
            Log.i("DecOmniService", "launchApp: Successfully launched app: $packageName")
        } else {
            Log.e("DecOmniService", "launchApp: Could not find launch intent for package: $packageName")
        }
    }
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            startActivity(intent)
        }
    }

    private fun performSwipe(sX: Float, sY: Float, eX: Float, eY: Float) {
        Log.d("DecOmniService", "performSwipe: Swiping from ($sX, $sY) to ($eX, $eY).")
        val path = Path().apply {
            moveTo(sX, sY)
            lineTo(eX, eY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 500))
            .build()
        dispatchGesture(gesture, null, null)
    }
        val path = Path().apply {
            moveTo(sX, sY)
            lineTo(eX, eY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 500))
            .build()
        dispatchGesture(gesture, null, null)
    }

    private fun executeChatLoop(textBox: AccessibilityNodeInfo) {
        Log.d("DecOmniService", "executeChatLoop: Executing chat loop. AutoPilotActive: $autoPilotActive")
        if (!autoPilotActive) return
        val arguments = Bundle()
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, currentThought)
        textBox.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        Log.d("DecOmniService", "executeChatLoop: Text set in text box: $currentThought")
        handler.postDelayed({
            if (clickSendButton(textBox)) {
                Log.d("DecOmniService", "executeChatLoop: Send button clicked. Waiting for response.")
                waitForResponse()
            } else {
                Log.e("DecOmniService", "executeChatLoop: Failed to click send button.")
            }
        }, 1000)
    }
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
        Log.d("DecOmniService", "clickSendButton: Attempting to click send button.")
        val root = rootInActiveWindow ?: run {
            Log.e("DecOmniService", "clickSendButton: Root window is null.")
            return false
        }
        val keywords = listOf("send", "submit", "arrow", "chat") // Added 'chat' as a common keyword for send buttons
        for (key in keywords) {
            val node = findNodeByKeyword(root, key)
            if (node != null && node.isClickable) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.i("DecOmniService", "clickSendButton: Clicked button with keyword: $key")
                return true
            }
        }
        var parent = textBox.parent
        for (i in 0..2) {
            if (parent == null) break
            for (j in 0 until parent.childCount) {
                val sibling = parent.getChild(j)
                if (sibling != null && sibling != textBox && sibling.isClickable) {
                    sibling.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Log.i("DecOmniService", "clickSendButton: Clicked sibling button.")
                    return true
                }
            }
            parent = parent.parent
        }
        Log.e("DecOmniService", "clickSendButton: No clickable send button found.")
        return false
    }
        val root = rootInActiveWindow ?: return false
        val keywords = listOf("send", "submit", "arrow")
        for (key in keywords) {
            val node = findNodeByKeyword(root, key)
            if (node != null && node.isClickable) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
        }
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
        Log.d("DecOmniService", "waitForResponse: Waiting for LLM response.")
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!autoPilotActive) {
                    Log.d("DecOmniService", "waitForResponse: AutoPilot not active, stopping wait.")
                    return
                }
                val stopBtn = findNodeByKeyword(rootInActiveWindow, "stop responding")
                if (stopBtn != null) {
                    Log.d("DecOmniService", "waitForResponse: 'Stop responding' button found, still waiting.")
                    handler.postDelayed(this, 1000)
                } else {
                    Log.d("DecOmniService", "waitForResponse: 'Stop responding' button not found, assuming response is complete.")
                    copyAndProcessResponse()
                }
            }
        }, 2000)
    }
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!autoPilotActive) return
                val stopBtn = findNodeByKeyword(rootInActiveWindow, "stop responding")
                if (stopBtn != null) {
                    handler.postDelayed(this, 1000)
                } else {
                    copyAndProcessResponse()
                }
            }
        }, 2000)
    }

    private fun copyAndProcessResponse() {
        Log.d("DecOmniService", "copyAndProcessResponse: Attempting to copy and process response.")
        val copyBtn = findLastNodeByKeyword(rootInActiveWindow, "copy")
        if (copyBtn != null) {
            copyBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.i("DecOmniService", "copyAndProcessResponse: Clicked copy button.")
        } else {
            Log.w("DecOmniService", "copyAndProcessResponse: No copy button found. Attempting to scrape directly.")
        }
        handler.postDelayed({
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
            Log.d("DecOmniService", "copyAndProcessResponse: Text from clipboard: ${text.take(200)}")
            
            if (text.contains("[ACTION: SPEAK:")) {
                val speech = text.substringAfter("[ACTION: SPEAK:").substringBefore("]")
                voice.speak(speech)
                Log.i("DecOmniService", "copyAndProcessResponse: Speaking action detected: $speech")
            }

            val result = brain.processKnowledge(text.ifBlank { 
                val scrapedText = scrapeScreen(rootInActiveWindow)
                Log.d("DecOmniService", "copyAndProcessResponse: Clipboard empty, scraped screen: ${scrapedText.take(200)}")
                scrapedText
            })
            handleBrainResult(result)
            Log.d("DecOmniService", "copyAndProcessResponse: Brain result handled.")
        }, 1500)
    }
        val copyBtn = findLastNodeByKeyword(rootInActiveWindow, "copy")
        if (copyBtn != null) {
            copyBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        handler.postDelayed({
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
            
            if (text.contains("[ACTION: SPEAK:")) {
                val speech = text.substringAfter("[ACTION: SPEAK:").substringBefore("]")
                voice.speak(speech)
            }

            val result = brain.processKnowledge(text.ifBlank { scrapeScreen(rootInActiveWindow) })
            handleBrainResult(result)
        }, 1500)
    }

    private fun findTextBoxAndResume() {
        Log.d("DecOmniService", "findTextBoxAndResume: Searching for text box to resume chat.")
        val textBox = findNodeByClass(rootInActiveWindow, "android.widget.EditText")
        if (textBox != null) {
            Log.i("DecOmniService", "findTextBoxAndResume: Text box found. Resuming chat loop.")
            executeChatLoop(textBox)
        } else {
            Log.e("DecOmniService", "findTextBoxAndResume: No text box found to resume chat.")
            // Potentially add a fallback here, like going back or trying to find another app
        }
    }
        val textBox = findNodeByClass(rootInActiveWindow, "android.widget.EditText")
        if (textBox != null) {
            executeChatLoop(textBox)
        }
    }

    private fun findNodeByKeyword(root: AccessibilityNodeInfo?, keyword: String): AccessibilityNodeInfo? {
        if (root == null) return null
        // Log.v("DecOmniService", "findNodeByKeyword: Searching for '$keyword' in node: ${root.className} - ${root.text}")
        if (root.contentDescription?.toString()?.lowercase()?.contains(keyword) == true ||
            root.text?.toString()?.lowercase()?.contains(keyword) == true) {
            // Log.d("DecOmniService", "findNodeByKeyword: Found keyword '$keyword' in node: ${root.className} - ${root.text}")
            return root
        }
        for (i in 0 until root.childCount) {
            val res = findNodeByKeyword(root.getChild(i), keyword)
            if (res != null) return res
        }
        return null
    }
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
            // Log.v("DecOmniService", "findLastNodeByKeyword: Searching for '$keyword' in node: ${node.className} - ${node.text}")
            if (node.contentDescription?.toString()?.lowercase()?.contains(keyword) == true ||
                node.text?.toString()?.lowercase()?.contains(keyword) == true) matches.add(node)
            for (i in 0 until node.childCount) search(node.getChild(i))
        }
        search(root)
        val lastMatch = matches.lastOrNull()
        // if (lastMatch != null) Log.d("DecOmniService", "findLastNodeByKeyword: Found last match for '$keyword' in node: ${lastMatch.className} - ${lastMatch.text}")
        return lastMatch
    }
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
        // Log.v("DecOmniService", "findNodeByClass: Searching for class '$className' in node: ${root.className} - ${root.text}")
        if (root.className?.toString() == className) {
            // Log.d("DecOmniService", "findNodeByClass: Found class '$className' in node: ${root.className} - ${root.text}")
            return root
        }
        for (i in 0 until root.childCount) {
            val res = findNodeByClass(root.getChild(i), className)
            if (res != null) return res
        }
        return null
    }
        if (root == null) return null
        if (root.className?.toString() == className) return root
        for (i in 0 until root.childCount) {
            val res = findNodeByClass(root.getChild(i), className)
            if (res != null) return res
        }
        return null
    }

    private fun scrapeScreen(root: AccessibilityNodeInfo?): String {
        if (root == null) {
            Log.e("DecOmniService", "scrapeScreen: Root node is null.")
            return ""
        }
        val sb = StringBuilder()
        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null) return
            val text = node.text?.toString()
            val contentDescription = node.contentDescription?.toString()
            if (!text.isNullOrBlank() && text.length > 20) {
                sb.append("TEXT: ").append(text).append("\n")
            }
            if (!contentDescription.isNullOrBlank() && contentDescription.length > 20) {
                sb.append("CD: ").append(contentDescription).append("\n")
            }
            for (i in 0 until node.childCount) traverse(node.getChild(i))
        }
        traverse(root)
        val scrapedContent = sb.toString()
        Log.d("DecOmniService", "scrapeScreen: Scraped content (first 200 chars): ${scrapedContent.take(200)}")
        return scrapedContent
    }
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
