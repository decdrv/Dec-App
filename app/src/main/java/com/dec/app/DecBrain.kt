package com.dec.app

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * DecBrain handles the core intelligence, memory storage, and prompt parsing.
 * It follows the "Parasitic Brain" concept, using external LLMs as its CPU.
 */
class DecBrain(private val context: Context) {

    private val brainDir: File by lazy {
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "DecBrain").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Parses the response from an LLM (Claude/Gemini) to extract Memory and Next Actions.
     */
    fun processKnowledge(text: String): BrainResult {
        val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        
        // 1. Extract Memory: [MEMORY: ...]
        val memory = extractTag(text, "[MEMORY:")
        if (memory.isNotBlank()) {
            saveMemoryToFile(memory, timeStamp)
        }

        // 2. Extract Web Search Action: [ACTION: SEARCH_WEB: ...]
        val searchQuery = extractTag(text, "[ACTION: SEARCH_WEB:")
        if (searchQuery.isNotBlank()) {
            return BrainResult.SearchWeb(searchQuery)
        }

        // 3. Extract App Opening Action: [ACTION: OPEN_APP: ...]
        val appName = extractTag(text, "[ACTION: OPEN_APP:")
        if (appName.isNotBlank()) {
            return BrainResult.OpenApp(appName)
        }

        // 4. Extract Next Prompt: [NEXT_PROMPT: ...]
        val nextPrompt = extractTag(text, "[NEXT_PROMPT:")
        if (nextPrompt.isNotBlank()) {
            return BrainResult.NextThought(nextPrompt)
        }

        return BrainResult.ContinueThinking("I understand. Please continue teaching me more about the current topic.")
    }

    private fun extractTag(text: String, tagStart: String): String {
        val startIdx = text.indexOf(tagStart)
        if (startIdx == -1) return ""
        val endIdx = text.indexOf("]", startIdx)
        if (endIdx == -1) return ""
        return text.substring(startIdx + tagStart.length, endIdx).trim()
    }

    private fun saveMemoryToFile(memory: String, timeStamp: String) {
        val fileStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val memFile = File(brainDir, "Memory_$fileStamp.txt")
        memFile.writeText("LOGGED AT: $timeStamp\n\n$memory")
    }

    sealed class BrainResult {
        data class NextThought(val prompt: String) : BrainResult()
        data class SearchWeb(val query: String) : BrainResult()
        data class OpenApp(val appName: String) : BrainResult()
        data class ContinueThinking(val fallbackPrompt: String) : BrainResult()
    }
}
