package io.github.oin.titanpocketkeyboard

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Filter
import android.widget.GridView
import android.widget.ImageButton
import android.widget.PopupWindow
import android.widget.TextView
import java.io.BufferedReader
import java.io.InputStreamReader

class EmojiAdapter(context: Context) : ArrayAdapter<Emoji>(context, 0) {

	private val allEmojis: List<Emoji>
	private var filteredEmojis: List<Emoji>
	private var emojiPopup: PopupWindow? = null
	private var popupShownTime: Long = 0

	init {
		allEmojis = loadEmojis(context)
		filteredEmojis = allEmojis
	}

	companion object {
		private const val POPUP_CLOSE_DELAY = 500 // milliseconds
	}

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


	override fun getCount(): Int = filteredEmojis.size

	override fun getItem(position: Int): Emoji? = filteredEmojis[position]

	override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
		val textView = if (convertView is TextView) {
			convertView
		} else {
			TextView(context).apply {
				textSize = 24f
				textAlignment = View.TEXT_ALIGNMENT_CENTER
			}
		}
		textView.text = getItem(position)?.character
		return textView
	}

	override fun getFilter(): Filter {
		return object : Filter() {
			override fun performFiltering(constraint: CharSequence?): FilterResults {
				val results = FilterResults()
				val query = constraint?.toString()?.lowercase()

				if (query.isNullOrEmpty()) {
					results.values = allEmojis
				} else {
					results.values = allEmojis.filter {
						it.name.lowercase().contains(query) || it.tags.any { tag -> tag.lowercase().contains(query) }
					}
				}
				return results
			}

			override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
				filteredEmojis = results?.values as? List<Emoji> ?: emptyList()
				notifyDataSetChanged()
			}
		}
	}

	fun show(anchor: View) {
		if (emojiPopup == null) {
			val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
			val emojiView = inflater.inflate(R.layout.emoji_picker, null)
			val emojiGrid = emojiView.findViewById<GridView>(R.id.emoji_grid)
			val backButton = emojiView.findViewById<ImageButton>(R.id.back_button)
			val searchBar = emojiView.findViewById<EditText>(R.id.search_bar)

			emojiGrid.adapter = this
			emojiGrid.setOnItemClickListener { _, _, position, _ ->
				val emoji = getItem(position)?.character
				(context as? InputMethodService)?.currentInputConnection?.commitText(emoji, 1)
				emojiPopup?.dismiss()
			}

			backButton.setOnClickListener {
				emojiPopup?.dismiss()
			}

			searchBar.addTextChangedListener(object : TextWatcher {
				override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
				override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
					filter.filter(s)
				}
				override fun afterTextChanged(s: Editable?) {}
			})

			searchBar.setOnKeyListener { _, keyCode, event ->
				if (keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
					if (event.action == android.view.KeyEvent.ACTION_DOWN) {
						return@setOnKeyListener true
					}
					if (event.action == android.view.KeyEvent.ACTION_UP) {
						if (filteredEmojis.isNotEmpty()) {
							getItem(0)?.character?.let {
								(context as? InputMethodService)?.currentInputConnection?.commitText(it, 1)
							}
							emojiPopup?.dismiss()
						}
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

			val height = context.resources.displayMetrics.heightPixels / 3
			emojiPopup = PopupWindow(
				emojiView,
				ViewGroup.LayoutParams.MATCH_PARENT,
				height
			).apply {
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