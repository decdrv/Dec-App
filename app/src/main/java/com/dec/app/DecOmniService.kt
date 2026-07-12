package com.dec.app

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.KeyEvent
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
    private var autoPilotActive = false
    private var lastSavedText = ""
    
    // 🧠 THE LIVING BRAIN: Dec's current thought
    private var currentThought = "Teach me a highly advanced concept about Artificial Intelligence. Explain it deeply. CRITICAL INSTRUCTION: At the very end of your response, you MUST generate the next logical, advanced question I should ask you to dive deeper into this topic. Format the next question EXACTLY like this: [NEXT: your question here]"

    override fun onServiceConnected() {
        super.onServiceConnected()
        getBrainDirectory()
        Toast.makeText(this, "🟢 LIVING BRAIN ONLINE (Type ..dec)", Toast.LENGTH_LONG).show()
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event != null && event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && event.action == KeyEvent.ACTION_DOWN) {
            autoPilotActive = false
            isProcessing = false
            Toast.makeText(this, "🛑 BRAIN ACTIVITY SUSPENDED!", Toast.LENGTH_LONG).show()
            return true
        }
        return super.onKeyEvent(event)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || isProcessing) return

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            val node = event.source ?: return
            val text = node.text?.toString() ?: ""

            if (text.trim().lowercase() == "..dec") {
                isProcessing = true
                autoPilotActive = true
                Toast.makeText(this, "⚡ CURIOSITY ENGINE ENGAGED!", Toast.LENGTH_LONG).show()
                startPerfectChatLoop(node)
            }
        }
    }

    override fun onInterrupt() {}

    // 📂 CHANGED TO DOWNLOADS FOLDER (Easiest to access, least restricted)
    private fun getBrainDirectory(): File {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "DecBrain")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    // 🧠 CREATES A NEW FILE FOR EVERY ANSWER!
    private fun processKnowledgeAndThink(text: String) {
        try {
            val dir = getBrainDirectory()
            // Create a unique filename using the exact time (e.g., Dec_Thought_10_30_15_AM.txt)
            val timeStamp = SimpleDateFormat("hh_mm_ss_a", Locale.getDefault()).format(Date())
            val memFile = File(dir, "Dec_Thought_$timeStamp.txt")

            // Write to a brand new file
            memFile.writeText("[LEARNED AT $timeStamp]\n\n$text")
            lastSavedText = text

            // ⚡ THE MAGIC: Find the [NEXT: ...] tag
            val startIndex = text.indexOf("[NEXT:")
            if (startIndex != -1) {
                val endIndex = text.indexOf("]", startIndex)
                if (endIndex != -1) {
                    val extractedQuestion = text.substring(startIndex + 6, endIndex).trim()
                    currentThought = "$extractedQuestion\n\nCRITICAL INSTRUCTION: At the very end of your response, you MUST generate the next logical question I should ask to dive deeper. Format it EXACTLY like this: [NEXT: your question here]"
                    Toast.makeText(this, "🧠 NEW FILE SAVED! Dec thought of next question!", Toast.LENGTH_SHORT).show()
                    return
                }
            }
            
            currentThought = "That was fascinating. Please explain another advanced aspect of this. Remember to end your response EXACTLY with [NEXT: your next question here]"
            Toast.makeText(this, "⚠️ NEW FILE SAVED! Format missed, using fallback.", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(this, "❌ File Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
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
        } else {
            Toast.makeText(this, "⚠️ No new text found to save!", Toast.LENGTH_SHORT).show()
            // Even if save fails, try to continue the loop
            if (autoPilotActive) {
                handler.postDelayed({ findTextBoxAndLoop() }, 3000)
            }
        }
    }

    private fun extractTextFromScreen(root: AccessibilityNodeInfo?): String {
        if (root == null) return ""
        val sb = StringBuilder()
        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null) return
            val text = node.text?.toString()
            val desc = node.contentDescription?.toString()
            if (!text.isNullOrBlank() && text.length > 30) sb.append(text).append("\n")
            else if (!desc.isNullOrBlank() && desc.length > 30) sb.append(desc).append("\n")
            for (i in 0 until node.childCount) traverse(node.getChild(i))
        }
        traverse(root)
        return sb.toString().trim()
    }

    private fun startPerfectChatLoop(textBox: AccessibilityNodeInfo) {
        if (!autoPilotActive) return

        if (checkLimitReached()) {
            Toast.makeText(this, "🛑 LIMIT REACHED! Brain sleeping.", Toast.LENGTH_LONG).show()
            autoPilotActive = false
            isProcessing = false
            return
        }

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

    private fun checkLimitReached(): Boolean {
        val root = rootInActiveWindow ?: return false
        val limit1 = findNodeByKeyword(root, "out of messages")
        val limit2 = findNodeByKeyword(root, "upgrade to pro")
        val limit3 = findNodeByKeyword(root, "limit reached")
        return limit1 != null || limit2 != null || limit3 != null
    }

    private fun pollForStopRespondingToAppear(attempts: Int) {
        if (!autoPilotActive || checkLimitReached()) {
            autoPilotActive = false
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
        if (!autoPilotActive || checkLimitReached()) {
            autoPilotActive = false
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
                
                handler.postDelayed({
                    saveToMemoryAndThink() // 🧠 READ, SAVE TO NEW FILE, AND GENERATE NEXT THOUGHT
                    
                    if (autoPilotActive) {
                        Toast.makeText(this, "🔄 Dec is thinking...", Toast.LENGTH_SHORT).show()
                        handler.postDelayed({ findTextBoxAndLoop() }, 3000)
                    } else {
                        isProcessing = false
                    }
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
