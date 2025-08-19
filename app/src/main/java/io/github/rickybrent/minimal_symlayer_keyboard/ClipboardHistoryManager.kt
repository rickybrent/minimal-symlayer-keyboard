package io.github.rickybrent.minimal_symlayer_keyboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object ClipboardHistoryManager {
    private val clipboardHistory = mutableListOf<Clipping>()
    private var clipboardManager: ClipboardManager? = null
    private lateinit var prefs: SharedPreferences

    private const val PREF_PINNED_CLIPPINGS = "pinned_clippings"

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
            prefs = context.getSharedPreferences(PREF_PINNED_CLIPPINGS, Context.MODE_PRIVATE)
            loadPinnedClippings()
        }
    }

    private fun loadPinnedClippings() {
        val pinnedItems = prefs.getStringSet(PREF_PINNED_CLIPPINGS, emptySet()) ?: emptySet()
        pinnedItems.forEach {
            clipboardHistory.add(Clipping(it, isPinned = true))
        }
    }

    private fun savePinnedClippings() {
        val pinnedItems = clipboardHistory.filter { it.isPinned }.map { it.text }.toSet()
        prefs.edit { putStringSet(PREF_PINNED_CLIPPINGS, pinnedItems) }
    }

    fun getHistory(): List<Clipping> {
        return clipboardHistory
    }

    fun togglePin(item: Clipping) {
        item.isPinned = !item.isPinned
        // Move pinned items to the top
        clipboardHistory.sortByDescending { it.isPinned }
        savePinnedClippings()
    }

    fun removeItem(item: Clipping) {
        clipboardHistory.remove(item)
    }

    fun paste(context: Context, text: String) {
        val clip = ClipData.newPlainText("pasted text", text)
        clipboardManager?.setPrimaryClip(clip)
    }
}