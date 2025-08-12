package io.github.oin.titanpocketkeyboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Filter
import android.widget.ImageButton
import android.widget.PopupWindow
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

class EmojiAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

	private val allEmojis: List<Emoji>
	private var recentEmojis: MutableList<Emoji>
	private var displayList: List<GridItem> = emptyList()

	private var emojiPopup: PopupWindow? = null
	private var popupShownTime: Long = 0
	private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

	companion object {
		private const val PREF_RECENT_EMOJI = "recent_emojis"
		private const val POPUP_CLOSE_DELAY = 500
		private const val GRID_SPAN_COUNT = 8
		private const val MAX_RECENTS = GRID_SPAN_COUNT

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

	override fun getItemViewType(position: Int): Int {
		return when (displayList[position]) {
			is EmojiItem -> TYPE_EMOJI
			is CategoryHeaderItem -> TYPE_CATEGORY_HEADER
			is RecentsHeaderItem -> TYPE_RECENTS_HEADER
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		val inflater = LayoutInflater.from(parent.context)
		return when (viewType) {
			TYPE_RECENTS_HEADER, TYPE_CATEGORY_HEADER -> {
				val view = inflater.inflate(R.layout.picker_category_header, parent, false)
				HeaderViewHolder(view)
			}
			else -> {
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
					(context as? InputMethodService)?.currentInputConnection?.commitText(item.emoji.character, 1)
					emojiPopup?.dismiss()
				}
			}
		}
	}

	override fun getItemCount(): Int = displayList.size

	// --- Helper and Logic Methods ---
	private fun loadEmojis(context: Context): List<Emoji> {
		val emojis = mutableListOf<Emoji>()
		val inputStream = context.resources.openRawResource(R.raw.all_emojis)
		val reader = BufferedReader(InputStreamReader(inputStream))
		reader.forEachLine { line ->
			val parts = line.split("\t")
			if (parts.size == 5) {
				val emoji = Emoji(
					character = parts[0],
					category = parts[1],
					subCategory = parts[2],
					name = parts[3],
					tags = parts[4].split("|")
				)
				emojis.add(emoji)
			}
		}
		return emojis
	}

	private fun loadRecents(): MutableList<Emoji> {
		val jsonString = prefs.getString(PREF_RECENT_EMOJI, "") ?: ""
		val recentChars = jsonString.split(",").filter { it.isNotEmpty() }
		// Find the full Emoji object from the master list
		return recentChars.mapNotNull { char -> allEmojis.find { it.character == char } }.toMutableList()
	}

	private fun saveRecents() {
		val jsonString = recentEmojis.joinToString(",") { it.character }
		prefs.edit().putString(PREF_RECENT_EMOJI, jsonString).apply()
	}

	private fun addEmojiToRecents(emoji: Emoji) {
		// Remove if it exists to move it to the front
		recentEmojis.remove(emoji)
		// Add to the front
		recentEmojis.add(0, emoji)
		// Trim the list
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
			recentEmojis.forEach { newDisplayList.add(EmojiItem(it)) }
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

	fun getFilter(): Filter {
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

	fun show(anchor: View) {
		if (emojiPopup == null) {
			val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
			val emojiView = inflater.inflate(R.layout.emoji_picker, null)
			val recyclerView = emojiView.findViewById<RecyclerView>(R.id.emoji_recycler_view)
			val backButton = emojiView.findViewById<ImageButton>(R.id.back_button)
			val searchBar = emojiView.findViewById<EditText>(R.id.search_bar)

			val layoutManager = GridLayoutManager(context, GRID_SPAN_COUNT)
			layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
				override fun getSpanSize(position: Int): Int {
					return when (displayList.getOrNull(position)) {
						is CategoryHeaderItem, is RecentsHeaderItem -> GRID_SPAN_COUNT
						else -> 1
					}
				}
			}
			recyclerView.layoutManager = layoutManager
			recyclerView.adapter = this

			backButton.setOnClickListener { emojiPopup?.dismiss() }
			searchBar.addTextChangedListener(object : TextWatcher {
				override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
				override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
					getFilter().filter(s)
				}
				override fun afterTextChanged(s: Editable?) {}
			})

			searchBar.setOnKeyListener { _, keyCode, event ->
				if (keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
					if (event.action == android.view.KeyEvent.ACTION_DOWN) { return@setOnKeyListener true }
					if (event.action == android.view.KeyEvent.ACTION_UP) {
						displayList.firstOrNull { it is EmojiItem }?.let {
							addEmojiToRecents((it as EmojiItem).emoji)
							(context as? InputMethodService)?.currentInputConnection?.commitText((it as EmojiItem).emoji.character, 1)
						}
						emojiPopup?.dismiss()
						return@setOnKeyListener true
					}
				}
				if (event.action == android.view.KeyEvent.ACTION_UP && keyCode == io.github.oin.titanpocketkeyboard.MP01_KEYCODE_EMOJI_PICKER) {
					if (System.currentTimeMillis() - popupShownTime > POPUP_CLOSE_DELAY) {
						emojiPopup?.dismiss()
					}
					return@setOnKeyListener true
				}
				return@setOnKeyListener false
			}

			val height = (context.resources.displayMetrics.heightPixels / 2.5).toInt()
			emojiPopup = PopupWindow(emojiView, ViewGroup.LayoutParams.MATCH_PARENT, height).apply {
				isFocusable = true
				isOutsideTouchable = true
			}
		}
		recentEmojis = loadRecents()
		updateDisplayList(allEmojis, isFiltering = false)
		emojiPopup?.contentView?.findViewById<EditText>(R.id.search_bar)?.setText("")


		if (emojiPopup?.isShowing == true) {
			emojiPopup?.dismiss()
		} else {
			popupShownTime = System.currentTimeMillis()
			emojiPopup?.showAtLocation(anchor, Gravity.BOTTOM, 0, 0)
			emojiPopup?.contentView?.findViewById<EditText>(R.id.search_bar)?.requestFocus()
		}
	}

	fun isShowing(): Boolean {
		return emojiPopup?.isShowing == true
	}
}