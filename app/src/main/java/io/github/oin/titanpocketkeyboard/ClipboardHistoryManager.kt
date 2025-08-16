package io.github.oin.titanpocketkeyboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

object ClipboardHistoryManager {
    private val clipboardHistory = mutableListOf<String>()
    private var clipboardManager: ClipboardManager? = null

    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        val clip = clipboardManager?.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString()
            if (!text.isNullOrEmpty() && !clipboardHistory.contains(text)) {
                clipboardHistory.add(0, text)
                // Optional: Limit the size of the history
                if (clipboardHistory.size > 50) {
                    clipboardHistory.removeAt(clipboardHistory.lastIndex)
                }
            }
        }
    }

    fun initialize(context: Context) {
        if (clipboardManager == null) {
            clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager?.addPrimaryClipChangedListener(clipboardListener)
        }
    }

    fun getHistory(): List<String> {
        return clipboardHistory
    }

    fun paste(context: Context, text: String) {
        val clip = ClipData.newPlainText("pasted text", text)
        clipboardManager?.setPrimaryClip(clip)
    }
}