package io.github.rickybrent.minimal_symlayer_keyboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.os.UserManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.recyclerview.widget.RecyclerView

class ClipboardHistoryAdapter(
    private val context: Context,
    private val onItemSelected: (String) -> Unit
) : RecyclerView.Adapter<ClipboardHistoryAdapter.ClipboardViewHolder>(), Filterable {

    private var fullHistory: MutableList<Clipping> = mutableListOf()
    private var filteredHistory: List<Clipping> = listOf()
    private var clipboardManager: ClipboardManager? = null

    private var _prefs: SharedPreferences? = null
    private val prefs: SharedPreferences?
        get() {
            if (_prefs == null) {
                _prefs = ContextCompat.getSystemService(context, UserManager::class.java)
                    ?.takeIf { it.isUserUnlocked }
                    ?.let { context.getSharedPreferences("clipboard_history", Context.MODE_PRIVATE) }
            }
            return _prefs
        }

    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        val clip = clipboardManager?.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString()
            if (!text.isNullOrEmpty() && fullHistory.none { it.text == text }) {
                fullHistory.add(0, Clipping(text))
                // Limit the size of the history, don't remove pinned items
                if (fullHistory.size > 50) {
                    val lastUnpinned = fullHistory.lastOrNull { !it.isPinned }
                    if (lastUnpinned != null) {
                        fullHistory.remove(lastUnpinned)
                    }
                }
                updateFilteredHistory()
            }
        }
    }

    init {
        initialize()
        loadPinnedClippings()
        updateFilteredHistory()
    }

    private fun initialize() {
        if (clipboardManager == null) {
            clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager?.addPrimaryClipChangedListener(clipboardListener)
        }
    }

    private fun loadPinnedClippings() {
        val pinnedItems = prefs?.getStringSet(PREF_PINNED_CLIPPINGS, emptySet()) ?: emptySet()
        pinnedItems.forEach {
            if (fullHistory.none { clipping -> clipping.text == it }) {
                fullHistory.add(Clipping(it, isPinned = true))
            }
        }
        fullHistory.sortByDescending { it.isPinned }
    }

    private fun savePinnedClippings() {
        prefs?.let {
            val pinnedItems = fullHistory.filter { it.isPinned }.map { it.text }.toSet()
            it.edit { putStringSet(PREF_PINNED_CLIPPINGS, pinnedItems) }
        }
    }

    fun getHistory(): List<Clipping> {
        return fullHistory
    }

    private fun togglePin(item: Clipping) {
        item.isPinned = !item.isPinned
        fullHistory.sortByDescending { it.isPinned }
        savePinnedClippings()
        updateFilteredHistory()
    }

    private fun removeItem(item: Clipping) {
        fullHistory.remove(item)
        updateFilteredHistory()
    }

    fun paste(text: String) {
        val clip = ClipData.newPlainText("pasted text", text)
        clipboardManager?.setPrimaryClip(clip)
    }

    fun refresh() {
        loadPinnedClippings()
        updateFilteredHistory()
    }

    private fun updateFilteredHistory() {
        filteredHistory = fullHistory
        notifyDataSetChanged()
    }

    class ClipboardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.clipboard_text)
        val pinIcon: ImageView = view.findViewById(R.id.pin_icon)
        val clearIcon: ImageView = view.findViewById(R.id.clear_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClipboardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.picker_item_clipping, parent, false)
        return ClipboardViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClipboardViewHolder, position: Int) {
        val item = filteredHistory[position]
        holder.textView.text = item.text
        holder.pinIcon.setImageResource(
            if (item.isPinned) R.drawable.ic_pin_filled else R.drawable.ic_pin_outline
        )

        holder.itemView.setOnClickListener {
            onItemSelected(item.text)
        }

        holder.pinIcon.setOnClickListener {
            togglePin(item)
        }

        holder.clearIcon.setOnClickListener {
            removeItem(item)
        }
    }

    override fun getItemCount(): Int = filteredHistory.size

    fun selectFirstItem() {
        if (filteredHistory.isNotEmpty()) {
            onItemSelected(filteredHistory[0].text)
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                val query = constraint?.toString()?.lowercase()
                results.values = if (query.isNullOrEmpty()) {
                    fullHistory
                } else {
                    fullHistory.filter {
                        it.text.lowercase().contains(query)
                    }
                }
                return results
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredHistory = results?.values as? List<Clipping> ?: emptyList()
                notifyDataSetChanged()
            }
        }
    }

    companion object {
        private const val PREF_PINNED_CLIPPINGS = "pinned_clippings"
    }
}