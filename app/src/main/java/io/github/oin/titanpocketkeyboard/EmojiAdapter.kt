package io.github.oin.titanpocketkeyboard

import android.annotation.SuppressLint
import android.content.Context
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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.BufferedReader
import java.io.InputStreamReader

// Sealed class to represent the different types of items in our grid
sealed class GridItem
data class EmojiItem(val emoji: Emoji) : GridItem()
data class CategoryHeaderItem(val category: String) : GridItem()

class EmojiAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

	private val allEmojis: List<Emoji>
	private var displayList: List<GridItem> = emptyList()

	private var emojiPopup: PopupWindow? = null
	private var popupShownTime: Long = 0

	companion object {
		private const val POPUP_CLOSE_DELAY = 500 // milliseconds
		private const val TYPE_EMOJI = 0
		private const val TYPE_HEADER = 1
		private const val GRID_SPAN_COUNT = 8
	}

	init {
		allEmojis = loadEmojis(context)
		updateDisplayList(allEmojis, isFiltering = false)
	}

	// --- ViewHolder Classes ---
	class EmojiViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val textView: TextView = view.findViewById(R.id.picker_item_emoji)
	}

	class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val textView: TextView = view.findViewById(R.id.picker_category_header)
	}

	// --- RecyclerView.Adapter Overrides ---
	override fun getItemViewType(position: Int): Int {
		return when (displayList[position]) {
			is EmojiItem -> TYPE_EMOJI
			is CategoryHeaderItem -> TYPE_HEADER
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		val inflater = LayoutInflater.from(parent.context)
		return if (viewType == TYPE_HEADER) {
			val view = inflater.inflate(R.layout.picker_category_header, parent, false)
			CategoryViewHolder(view)
		} else {
			val view = inflater.inflate(R.layout.picker_item_emoji, parent, false)
			EmojiViewHolder(view)
		}
	}

	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		when (val item = displayList[position]) {
			is CategoryHeaderItem -> (holder as CategoryViewHolder).textView.text = item.category
			is EmojiItem -> {
				val emojiHolder = holder as EmojiViewHolder
				emojiHolder.textView.text = item.emoji.character
				emojiHolder.itemView.setOnClickListener {
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

	@SuppressLint("NotifyDataSetChanged")
	private fun updateDisplayList(emojis: List<Emoji>, isFiltering: Boolean) {
		val newDisplayList = mutableListOf<GridItem>()
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
					return if (getItemViewType(position) == TYPE_HEADER) GRID_SPAN_COUNT else 1
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