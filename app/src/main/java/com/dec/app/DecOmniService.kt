package com.dec.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import android.app.SearchManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DecOmniService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var isProcessing = false
    private var autoPilotActive = false
    private var lastSavedText = ""
    private var currentThought = ""
    private var isFirstThought = true
    
    // State Machine to track what Dec is currently doing
    private var currentState = "CHATTING" // Can be: CHATTING, SEARCHING_WEB

    override fun onServiceConnected() {
        super.onServiceConnected()
        getBrainDirectory()
        Toast.makeText(this, "🌐 GOD-MODE OMNI-ENGINE ONLINE", Toast.LENGTH_LONG).show()
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event != null && event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && event.action == KeyEvent.ACTION_DOWN) {
            autoPilotActive = false
            isProcessing = false
            currentState = "CHATTING"
            Toast.makeText(this, "🛑 EMERGENCY STOP ACTIVATED!", Toast.LENGTH_LONG).show()
            return true
        }
        return super.onKeyEvent(event)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !autoPilotActive && !isProcessing) {
            // Trigger to start the engine
            if (event?.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
                val node = event.source ?: return
                val text = node.text?.toString() ?: ""
                if (text.trim().lowercase() == "..dec") {
                    startGodMode(node)
                }
            }
            return
        }
    }

    override fun onInterrupt() {}

    private fun getBrainDirectory(): File {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "DecBrain")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun startGodMode(textBox: AccessibilityNodeInfo) {
        isProcessing = true
        autoPilotActive = true
        
        if (isFirstThought) {
            val prefs = getSharedPreferences("DecPrefs", Context.MODE_PRIVATE)
            val topic = prefs.getString("custom_topic", "Latest Technology News") ?: "Latest Technology News"
            
            currentThought = """
                You are DEC, an autonomous AI agent with INTERNET ACCESS.
                Mission: Master the topic "$topic".
                
                You can command me (the Android system) to do things.
                If you know the answer, teach me and end with:
                [MEMORY: <dense summary>]
                [NEXT_PROMPT: <next question>]
                
                If you NEED live internet data, command me to search the web by ending EXACTLY with:
                [ACTION: SEARCH_WEB: <your search query>]
            """.trimIndent()
            isFirstThought = false
        }
        
        Toast.makeText(this, "⚡ GOD-MODE ENGAGED!", Toast.LENGTH_LONG).show()
        startPerfectChatLoop(textBox)
    }

    // 🧠 THE MASTER PARSER (Understands Memory, Prompts, and Actions)
    private fun processKnowledgeAndThink(text: String) {
        try {
            val dir = getBrainDirectory()
            val timeStamp = SimpleDateFormat("hh_mm_ss_a", Locale.getDefault()).format(Date())
            val memFile = File(dir, "Dec_Memory_$timeStamp.txt")

            // 1. Extract Memory
            val memStart = text.indexOf("[MEMORY:")
            if (memStart != -1) {
                val memEnd = text.indexOf("]", memStart)
                if (memEnd != -1) {
                    val memoryToSave = text.substring(memStart + 8, memEnd).trim()
                    memFile.writeText("[LOGGED AT $timeStamp]\n\n$memoryToSave")
                }
            }
            lastSavedText = text

            // 2. Check for Web Search Action
            val actionStart = text.indexOf("[ACTION: SEARCH_WEB:")
            if (actionStart != -1) {
                val actionEnd = text.indexOf("]", actionStart)
                if (actionEnd != -1) {
                    val query = text.substring(actionStart + 20, actionEnd).trim()
                    executeWebSearch(query)
                    return // Stop chat loop, we are going to the web!
                }
            }

            // 3. Extract Next Prompt
            val promptStart = text.indexOf("[NEXT_PROMPT:")
            if (promptStart != -1) {
                val promptEnd = text.indexOf("]", promptStart)
                if (promptEnd != -1) {
                    val nextPrompt = text.substring(promptStart + 13, promptEnd).trim()
                    currentThought = "$nextPrompt\n\nRemember: End with [MEMORY: ...] and [NEXT_PROMPT: ...] OR [ACTION: SEARCH_WEB: ...]"
                    Toast.makeText(this, "🧠 Thinking next thought...", Toast.LENGTH_SHORT).show()
                }
            } else {
                currentThought = "Continue explaining. Remember your command formats."
            }

        } catch (e: Exception) {}
    }

    // 🌐 THE WEB SURFER: Opens Google Search automatically
    private fun executeWebSearch(query: String) {
        Toast.makeText(this, "🌐 SEARCHING WEB: $query", Toast.LENGTH_LONG).show()
        currentState = "SEARCHING_WEB"
        
        // Launch Android's built-in Web Search Intent
        val intent = Intent(Intent.ACTION_WEB_SEARCH)
        intent.putExtra(SearchManager.QUERY, query)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)

        // Wait 6 seconds for results to load, then scrape and go back!
        handler.postDelayed({
            scrapeWebAndReturn()
        }, 6000)
    }

    // 🕷️ THE SCRAPER & NAVIGATOR: Reads web, presses BACK button
    private fun scrapeWebAndReturn() {
        if (!autoPilotActive) return
        
        Toast.makeText(this, "👁️ SCRAPING WEB DATA...", Toast.LENGTH_SHORT).show()
        
        // Scroll down slightly to get more text (Human-like gesture)
        performScroll()
        
        handler.postDelayed({
            val webData = extractTextFromScreen(rootInActiveWindow)
            
            // Press the GLOBAL BACK BUTTON to return to Claude!
            performGlobalAction(GLOBAL_ACTION_BACK)
            Toast.makeText(this, "🔙 RETURNING TO CLAUDE...", Toast.LENGTH_SHORT).show()
            
            currentState = "CHATTING"
            
            // Feed the scraped data back to Claude
            currentThought = """
                I searched the web. Here is the raw data I found:
                ---
                ${webData.take(1500)} // Taking first 1500 chars to avoid limits
                ---
                Analyze this data, teach me what you learned, and end with [MEMORY: ...] and [NEXT_PROMPT: ...]
            """.trimIndent()
            
            // Wait 3 seconds for Claude to reopen, then type
            handler.postDelayed({ findTextBoxAndLoop() }, 3000)
            
        }, 2000) // Wait 2 seconds after scroll
    }

    // 👆 THE SWIPER: Simulates a human finger swiping up (scrolling down)
    private fun performScroll() {
        val displayMetrics = resources.displayMetrics
        val middleX = displayMetrics.widthPixels / 2f
        val startY = displayMetrics.heightPixels * 0.8f // Start near bottom
        val endY = displayMetrics.heightPixels * 0.2f   // Swipe to top

        val path = Path()
        path.moveTo(middleX, startY)
        path.lineTo(middleX, endY)

        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 500)) // 500ms swipe
        dispatchGesture(gestureBuilder.build(), null, null)
    }

    private fun saveToMemoryAndThink() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard.primaryClip
        var newText = ""

        if (clipData != null && clipData.itemCount > 0) {
            newText = clipData.getItemAt(0).text?.toString() ?: ""
        }

        if (newText.isBlank() || newText == lastSavedText) {
            val screenText = extractTextFromScreen(rootInActiveWindow)
            if (screenText.isNotBlank()) newText = screenText
        }

        if (newText.isNotBlank() && newText != lastSavedText) {
            processKnowledgeAndThink(newText)
        }
        
        if (autoPilotActive && currentState == "CHATTING") {
            handler.postDelayed({ findTextBoxAndLoop() }, 3000)
        }
    }

    private fun extractTextFromScreen(root: AccessibilityNodeInfo?): String {
        if (root == null) return ""
        val sb = StringBuilder()
        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null) return
            val text = node.text?.toString()
            val desc = node.contentDescription?.toString()
            if (!text.isNullOrBlank() && text.length > 20) sb.append(text).append("\n")
            else if (!desc.isNullOrBlank() && desc.length > 20) sb.append(desc).append("\n")
            for (i in 0 until node.childCount) traverse(node.getChild(i))
        }
        traverse(root)
        return sb.toString().trim()
    }

    private fun startPerfectChatLoop(textBox: AccessibilityNodeInfo) {
        if (!autoPilotActive) return

        val arguments = Bundle()
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, currentThought)
        textBox.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        
        handler.postDelayed({
            val clicked = tryClickingSend(textBox)
            if (clicked) {
                pollForStopRespondingToAppear(0)
            } else {
                autoPilotActive = false
                isProcessing = false
            }
        }, 1000)
    }

    private fun tryClickingSend(textBox: AccessibilityNodeInfo): Boolean {
        val root = rootInActiveWindow ?: return false
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
        var parent = textBox.parent
        for (i in 0..2) {
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

    private fun pollForStopRespondingToAppear(attempts: Int) {
        if (!autoPilotActive) return
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
        if (!autoPilotActive) return
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
                }
                
                handler.postDelayed({
                    saveToMemoryAndThink()
                }, 2000)
                
            }, 1000)
        }
    }

    private fun findTextBoxAndLoop() {
        if (!autoPilotActive) return
        val root = rootInActiveWindow
        if (root != null) {
            val textBox = findNodeByClass(root, "android.widget.EditText")
            if (textBox != null) {
                startPerfectChatLoop(textBox)
            } else {
                autoPilotActive = false
                isProcessing = false
            }
        } else {
            autoPilotActive = false
            isProcessing = false
        }
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
