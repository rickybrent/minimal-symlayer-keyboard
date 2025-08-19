package io.github.rickybrent.minimal_symlayer_keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.BufferedReader
import java.io.InputStreamReader

// --- Data Structures ---
sealed class GridItem
data class EmojiItem(val emoji: Emoji) : GridItem()
data class CategoryHeaderItem(val category: String) : GridItem()
object RecentsHeaderItem : GridItem()

class EmojiAdapter(
    private val context: Context,
    private val onEmojiSelected: (Emoji) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), Filterable {

    private val allEmojis: List<Emoji>
    private var recentEmojis: MutableList<Emoji>
    private var displayList: List<GridItem> = emptyList()
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    companion object {
        private const val PREF_RECENT_EMOJI = "recent_emojis"
        const val GRID_SPAN_COUNT = 8
        private const val MAX_RECENTS = GRID_SPAN_COUNT * 2 // Allow for two rows of recents

        private const val TYPE_EMOJI = 0
        private const val TYPE_CATEGORY_HEADER = 1
        private const val TYPE_RECENTS_HEADER = 2
    }

    init {
        allEmojis = loadEmojis(context)
        recentEmojis = loadRecents()
        updateDisplayList(allEmojis, isFiltering = false)
    }

    // --- ViewHolder Classes ---
    class EmojiViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.picker_item_emoji)
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.picker_category_header)
    }

    // --- RecyclerView.Adapter Overrides ---
    override fun getItemViewType(position: Int): Int {
        return when (displayList.getOrNull(position)) {
            is EmojiItem -> TYPE_EMOJI
            is CategoryHeaderItem -> TYPE_CATEGORY_HEADER
            is RecentsHeaderItem -> TYPE_RECENTS_HEADER
            else -> -1 // Should not happen
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_RECENTS_HEADER, TYPE_CATEGORY_HEADER -> {
                val view = inflater.inflate(R.layout.picker_category_header, parent, false)
                HeaderViewHolder(view)
            }
            else -> { // TYPE_EMOJI
                val view = inflater.inflate(R.layout.picker_item_emoji, parent, false)
                EmojiViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = displayList[position]) {
            is RecentsHeaderItem -> (holder as HeaderViewHolder).textView.text = "Recents"
            is CategoryHeaderItem -> (holder as HeaderViewHolder).textView.text = item.category
            is EmojiItem -> {
                val emojiHolder = holder as EmojiViewHolder
                emojiHolder.textView.text = item.emoji.character
                emojiHolder.itemView.setOnClickListener {
                    addEmojiToRecents(item.emoji)
                    onEmojiSelected(item.emoji)
                }
            }
        }
    }

    override fun getItemCount(): Int = displayList.size

    // --- Public Helper Methods ---
    fun getSpanSizeLookup(): GridLayoutManager.SpanSizeLookup {
        return object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                // Make all headers span all columns
                return when (displayList.getOrNull(position)) {
                    is CategoryHeaderItem, is RecentsHeaderItem -> GRID_SPAN_COUNT
                    else -> 1
                }
            }
        }
    }

    fun selectFirstEmoji() {
        displayList.firstOrNull { it is EmojiItem }?.let {
            val emoji = (it as EmojiItem).emoji
            addEmojiToRecents(emoji)
            onEmojiSelected(emoji)
        }
    }

    fun refresh() {
        recentEmojis = loadRecents()
        updateDisplayList(allEmojis, isFiltering = false)
    }

    // --- Persistence and Data Handling ---
    private fun loadEmojis(context: Context): List<Emoji> {
        val emojis = mutableListOf<Emoji>()
        val inputStream = context.resources.openRawResource(R.raw.all_emojis)
        BufferedReader(InputStreamReader(inputStream)).forEachLine { line ->
            val parts = line.split("\t")
            if (parts.size == 5) {
                emojis.add(Emoji(parts[0], parts[1], parts[2], parts[3], parts[4].split("|")))
            }
        }
        return emojis
    }

    private fun loadRecents(): MutableList<Emoji> {
        val jsonString = prefs.getString(PREF_RECENT_EMOJI, "") ?: ""
        val recentChars = jsonString.split(",").filter { it.isNotEmpty() }
        return recentChars.mapNotNull { char -> allEmojis.find { it.character == char } }.toMutableList()
    }

    private fun saveRecents() {
        val jsonString = recentEmojis.joinToString(",") { it.character }
        prefs.edit().putString(PREF_RECENT_EMOJI, jsonString).apply()
    }

    private fun addEmojiToRecents(emoji: Emoji) {
        recentEmojis.remove(emoji)
        recentEmojis.add(0, emoji)
        while (recentEmojis.size > MAX_RECENTS) {
            recentEmojis.removeAt(recentEmojis.lastIndex)
        }
        saveRecents()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateDisplayList(emojis: List<Emoji>, isFiltering: Boolean) {
        val newDisplayList = mutableListOf<GridItem>()
        if (recentEmojis.isNotEmpty() && !isFiltering) {
            newDisplayList.add(RecentsHeaderItem)
            newDisplayList.addAll(recentEmojis.map { EmojiItem(it) })
        }

        var lastCategory = ""
        emojis.forEach { emoji ->
            if (!isFiltering && emoji.category != lastCategory) {
                newDisplayList.add(CategoryHeaderItem(emoji.category))
                lastCategory = emoji.category
            }
            newDisplayList.add(EmojiItem(emoji))
        }
        this.displayList = newDisplayList
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                val query = constraint?.toString()?.lowercase()
                results.values = if (query.isNullOrEmpty()) {
                    allEmojis
                } else {
                    allEmojis.filter {
                        it.name.lowercase().contains(query) || it.tags.any { tag -> tag.lowercase().contains(query) }
                    }
                }
                return results
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                val filteredEmojis = results?.values as? List<Emoji> ?: emptyList()
                updateDisplayList(filteredEmojis, !constraint.isNullOrEmpty())
            }
        }
    }
}
