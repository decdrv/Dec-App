package com.dec.app

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.content.ClipboardManager
import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DecOmniService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var isProcessing = false
    private var lastSavedText = "" // Prevents saving the exact same thing twice

    override fun onServiceConnected() {
        super.onServiceConnected()
        getBrainDirectory()
        Toast.makeText(this, "🟢 DEC BRAIN ONLINE (Type ..dec)", Toast.LENGTH_LONG).show()
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

    private fun getBrainDirectory(): File {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "DecBrain")
        if (!dir.exists()) dir.mkdirs()

        val memFile = File(dir, "dec_knowledge.txt")
        if (!memFile.exists()) memFile.writeText("--- DEC KNOWLEDGE VAULT ---\n")

        return dir
    }

    private fun getNextQuestion(): String {
        try {
            val dir = getBrainDirectory()
            val qFile = File(dir, "dec_questions.txt")
            
            if (!qFile.exists()) {
                qFile.writeText("Explain quantum computing in 1 simple sentence.\nWhat is the future of AI?\nGive me a unique startup idea.\nHow does a black hole work?")
            }
            
            val lines = qFile.readLines().filter { it.isNotBlank() }
            if (lines.isEmpty()) return "Tell me a random technology fact."
            
            val prefs = getSharedPreferences("DecPrefs", Context.MODE_PRIVATE)
            val currentIndex = prefs.getInt("q_index", 0)
            
            val question = lines[currentIndex % lines.size]
            prefs.edit().putInt("q_index", currentIndex + 1).apply()
            
            return question
        } catch (e: Exception) {
            return "Tell me a random fact."
        }
    }

    // 🧠 UPGRADED MEMORY SAVER (With Timestamps & Duplicate Check)
    private fun saveToMemory() {
        try {
            val dir = getBrainDirectory()
            val memFile = File(dir, "dec_knowledge.txt")
            val timeStamp = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())

            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = clipboard.primaryClip
            var newText = ""

            // Try getting text from Clipboard
            if (clipData != null && clipData.itemCount > 0) {
                newText = clipData.getItemAt(0).text?.toString() ?: ""
            }

            // If Clipboard is empty or has old data, use X-Ray Vision
            if (newText.isBlank() || newText == lastSavedText) {
                val screenText = extractTextFromScreen(rootInActiveWindow)
                if (screenText.isNotBlank()) {
                    newText = screenText
                }
            }

            // Save only if we have text AND it's not a duplicate
            if (newText.isNotBlank() && newText != lastSavedText) {
                memFile.appendText("\n\n[SAVED AT $timeStamp]\n$newText")
                lastSavedText = newText // Update memory
                Toast.makeText(this, "🧠 SAVED AT $timeStamp!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "⚠️ No new data found to save!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Save Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun extractTextFromScreen(root: AccessibilityNodeInfo?): String {
        if (root == null) return ""
        val sb = StringBuilder()
        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null) return
            val text = node.text?.toString()
            val desc = node.contentDescription?.toString()
            
            if (!text.isNullOrBlank() && text.length > 30) {
                sb.append(text).append("\n")
            } else if (!desc.isNullOrBlank() && desc.length > 30) {
                sb.append(desc).append("\n")
            }
            for (i in 0 until node.childCount) traverse(node.getChild(i))
        }
        traverse(root)
        return sb.toString().trim()
    }

    private fun startPerfectChatLoop(textBox: AccessibilityNodeInfo) {
        if (checkLimitReached()) {
            Toast.makeText(this, "🛑 LIMIT REACHED! Dec is sleeping.", Toast.LENGTH_LONG).show()
            isProcessing = false
            return
        }

        val dynamicQuestion = getNextQuestion()
        val arguments = Bundle()
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, dynamicQuestion)
        textBox.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        
        handler.postDelayed({
            val clicked = tryClickingSend(textBox)
            if (clicked) {
                pollForStopRespondingToAppear(0)
            } else {
                Toast.makeText(this, "❌ Send button hidden!", Toast.LENGTH_SHORT).show()
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

    private fun checkLimitReached(): Boolean {
        val root = rootInActiveWindow ?: return false
        val limit1 = findNodeByKeyword(root, "out of messages")
        val limit2 = findNodeByKeyword(root, "upgrade to pro")
        val limit3 = findNodeByKeyword(root, "limit reached")
        return limit1 != null || limit2 != null || limit3 != null
    }

    private fun pollForStopRespondingToAppear(attempts: Int) {
        if (checkLimitReached()) {
            isProcessing = false
            return
        }
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
        if (checkLimitReached()) {
            isProcessing = false
            return
        }
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
                
                // ⚡ Wait 2 seconds to ensure Clipboard is updated, then Save!
                handler.postDelayed({
                    saveToMemory()
                    isProcessing = false
                }, 2000)
                
            }, 1000)
        }
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
