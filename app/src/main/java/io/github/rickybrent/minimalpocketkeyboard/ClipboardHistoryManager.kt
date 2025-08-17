package io.github.rickybrent.minimalpocketkeyboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

object ClipboardHistoryManager {
    private val clipboardHistory = mutableListOf<Clipping>()
    private var clipboardManager: ClipboardManager? = null

    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        val clip = clipboardManager?.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString()
            if (!text.isNullOrEmpty() && clipboardHistory.none { it.text == text }) {
                clipboardHistory.add(0, Clipping(text))
                // Limit the size of the history, don't remove pinned items
                if (clipboardHistory.size > 50) {
                    val lastUnpinned = clipboardHistory.lastOrNull { !it.isPinned }
                    if (lastUnpinned != null) {
                        clipboardHistory.remove(lastUnpinned)
                    }
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

    fun getHistory(): List<Clipping> {
        return clipboardHistory
    }

    fun togglePin(item: Clipping) {
        item.isPinned = !item.isPinned
        // Move pinned items to the top
        clipboardHistory.sortByDescending { it.isPinned }
    }

    fun removeItem(item: Clipping) {
        clipboardHistory.remove(item)
    }

    fun paste(context: Context, text: String) {
        val clip = ClipData.newPlainText("pasted text", text)
        clipboardManager?.setPrimaryClip(clip)
    }
}