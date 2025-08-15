package io.github.oin.titanpocketkeyboard

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.PopupWindow
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PickerManager(private val context: Context, private val service: InputMethodService) {

    private var popupWindow: PopupWindow? = null
    private var emojiAdapter: EmojiAdapter? = null
    private var symbolAdapter: SymbolAdapter? = null
    private var popupShownTime: Long = 0

    private lateinit var contentArea: FrameLayout
    private lateinit var searchBar: EditText
    private lateinit var symButton: ImageButton

    enum class ViewType {
        EMOJI, SYMBOL
    }

    // Define the shared key listener as a property of the class
    private val keyListener = View.OnKeyListener { _, keyCode, event ->
        // Delay to prevent accidental closing immediately after opening
        if (System.currentTimeMillis() - popupShownTime < 500) {
            return@OnKeyListener true // Consume the event
        }

        if (event.action == KeyEvent.ACTION_UP) {
            when (keyCode) {
                KeyEvent.KEYCODE_SYM -> {
                    switchToView(ViewType.SYMBOL)
                    return@OnKeyListener true
                }
                MP01_KEYCODE_EMOJI_PICKER -> {
                    popupWindow?.dismiss()
                    return@OnKeyListener true
                }
            }
        }

        // Handle 'Enter' key only if the search bar has focus
        if (keyCode == KeyEvent.KEYCODE_ENTER && searchBar.hasFocus()) {
            if (event.action == KeyEvent.ACTION_DOWN) return@OnKeyListener true // Consume down event
            if (event.action == KeyEvent.ACTION_UP) {
                getEmojiAdapter().selectFirstEmoji() // This will also dismiss the popup
                return@OnKeyListener true
            }
        }
        false // Don't consume other events
    }

    private fun getEmojiAdapter(): EmojiAdapter {
        if (emojiAdapter == null) {
            emojiAdapter = EmojiAdapter(context) { emoji ->
                service.currentInputConnection?.commitText(emoji.character, 1)
                popupWindow?.dismiss()
            }
        }
        return emojiAdapter!!
    }

    private fun getSymbolAdapter(): SymbolAdapter {
        if (symbolAdapter == null) {
            symbolAdapter = SymbolAdapter(service) { keyCode ->
                service.onSymKey(KeyEvent(KeyEvent.ACTION_DOWN, keyCode), true)
                service.onSymKey(KeyEvent(KeyEvent.ACTION_UP, keyCode), false)
            }
        }
        return symbolAdapter!!
    }

    fun show() {
        if (popupWindow == null) {
            setupPopupWindow()
        }

        if (popupWindow?.isShowing == true) {
            popupWindow?.dismiss()
        } else {
            popupShownTime = System.currentTimeMillis()
            switchToView(ViewType.EMOJI) // Default to emoji view
            popupWindow?.showAtLocation(service.window.window!!.decorView, Gravity.BOTTOM, 0, 0)
        }
    }

    private fun setupPopupWindow() {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val containerView = inflater.inflate(R.layout.picker_container, null)

        contentArea = containerView.findViewById(R.id.picker_content_area)
        searchBar = containerView.findViewById(R.id.search_bar)
        val backButton = containerView.findViewById<ImageButton>(R.id.back_button)
        symButton = containerView.findViewById(R.id.sym_button)

        backButton.setOnClickListener { popupWindow?.dismiss() }
        symButton.setOnClickListener { switchToView(ViewType.SYMBOL) }

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                getEmojiAdapter().filter.filter(s)
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        searchBar.addTextChangedListener(textWatcher)

        // Attach the same listener to both views
        containerView.isFocusableInTouchMode = true
        containerView.setOnKeyListener(keyListener)
        searchBar.setOnKeyListener(keyListener)

        val height = (context.resources.displayMetrics.heightPixels / 2.5).toInt()
        popupWindow = PopupWindow(
            containerView,
            FrameLayout.LayoutParams.MATCH_PARENT,
            height
        ).apply {
            isFocusable = true
            isOutsideTouchable = true
        }
    }

    private fun switchToView(viewType: ViewType) {
        contentArea.removeAllViews()
        val recyclerView = RecyclerView(context)
        contentArea.addView(recyclerView)

        if (viewType == ViewType.EMOJI) {
            searchBar.visibility = View.VISIBLE
            symButton.visibility = View.VISIBLE
            val adapter = getEmojiAdapter()
            adapter.refresh()
            val layoutManager = GridLayoutManager(context, EmojiAdapter.GRID_SPAN_COUNT)
            layoutManager.spanSizeLookup = adapter.getSpanSizeLookup()
            recyclerView.layoutManager = layoutManager
            recyclerView.adapter = adapter
            searchBar.requestFocus()
        } else { // SYMBOL
            searchBar.visibility = View.GONE
            symButton.visibility = View.GONE
            val adapter = getSymbolAdapter()
            recyclerView.layoutManager = GridLayoutManager(context, 10)
            recyclerView.adapter = adapter
            // Request focus on the container so it can receive key events
            popupWindow?.contentView?.requestFocus()
        }
    }

    fun isShowing(): Boolean = popupWindow?.isShowing ?: false
}
