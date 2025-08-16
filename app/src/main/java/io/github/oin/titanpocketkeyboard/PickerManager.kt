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
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PickerManager(private val context: Context, private val service: InputMethodService) {

    private var popupWindow: PopupWindow? = null
    private var emojiAdapter: EmojiAdapter? = null
    private var symbolAdapter: SymbolAdapter? = null
    private var clipboardAdapter: ClipboardAdapter? = null
    private var popupShownTime: Long = 0
    private var initialPressComplete = false
    private var activeTextWatcher: TextWatcher? = null
    private var currentView: ViewType = ViewType.EMOJI

    private lateinit var contentArea: FrameLayout
    private lateinit var searchBar: EditText
    private lateinit var symButton: ImageButton
    private lateinit var clipboardButton: ImageButton
    private lateinit var emptyClipboardMessage: TextView
    private lateinit var recyclerView: RecyclerView

    enum class ViewType {
        EMOJI, SYMBOL, CLIPBOARD
    }

    // Define the shared key listener as a property of the class
    private val keyListener = View.OnKeyListener { _, keyCode, event ->
        if (event.action == KeyEvent.ACTION_UP && keyCode == MP01_KEYCODE_EMOJI_PICKER) {
            // Prevent the key up event from the opening hotkey from immediately closing the popup.
            if (!initialPressComplete && System.currentTimeMillis() - popupShownTime < 1000) {
                initialPressComplete = true
                return@OnKeyListener true // Consume the event
            }
            popupWindow?.dismiss()
            return@OnKeyListener true
        }

        if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_SYM) {
            switchToView(ViewType.SYMBOL)
            return@OnKeyListener true
        }

        // Handle 'Enter' key only if the search bar has focus
        if (keyCode == KeyEvent.KEYCODE_ENTER && searchBar.hasFocus()) {
            if (event.action == KeyEvent.ACTION_DOWN) return@OnKeyListener true // Consume down event
            if (event.action == KeyEvent.ACTION_UP) {
                // This will insert text or an emoji and dismiss the popup.
                when (currentView) {
                    ViewType.EMOJI -> getEmojiAdapter().selectFirstEmoji()
                    ViewType.CLIPBOARD -> getClipboardAdapter().selectFirstItem()
                    else -> {}
                }
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

    private fun getClipboardAdapter(): ClipboardAdapter {
        if (clipboardAdapter == null) {
            clipboardAdapter = ClipboardAdapter { text ->
                service.currentInputConnection?.commitText(text, 1)
                popupWindow?.dismiss()
            }
        }
        return clipboardAdapter!!
    }


    fun show(startingView: ViewType = ViewType.EMOJI) {
        if (popupWindow == null) {
            setupPopupWindow()
        }

        if (popupWindow?.isShowing == true) {
            popupWindow?.dismiss()
        } else {
            initialPressComplete = false
            popupShownTime = System.currentTimeMillis()
            switchToView(startingView) // Default to emoji view
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
        clipboardButton = containerView.findViewById(R.id.clipboard_button)
        emptyClipboardMessage = containerView.findViewById(R.id.empty_clipboard_message)

        // Create and add the RecyclerView here
        recyclerView = RecyclerView(context)
        contentArea.addView(recyclerView)


        backButton.setOnClickListener { popupWindow?.dismiss() }
        symButton.setOnClickListener { switchToView(ViewType.SYMBOL) }
        clipboardButton.setOnClickListener { switchToView(ViewType.CLIPBOARD) }

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
        currentView = viewType

        recyclerView.visibility = View.GONE
        emptyClipboardMessage.visibility = View.GONE

        // Remove the searchBar watcher before switching
        activeTextWatcher?.let { searchBar.removeTextChangedListener(it) }

        when (viewType) {
            ViewType.EMOJI -> {
                recyclerView.visibility = View.VISIBLE
                searchBar.visibility = View.VISIBLE
                symButton.visibility = View.VISIBLE
                clipboardButton.visibility = View.VISIBLE
                val adapter = getEmojiAdapter()
                adapter.refresh()
                val layoutManager = GridLayoutManager(context, EmojiAdapter.GRID_SPAN_COUNT)
                layoutManager.spanSizeLookup = adapter.getSpanSizeLookup()
                recyclerView.layoutManager = layoutManager
                recyclerView.adapter = adapter
                searchBar.requestFocus()
                searchBar.hint = "Search emoji"
                activeTextWatcher = object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        adapter.filter.filter(s)
                    }
                    override fun afterTextChanged(s: Editable?) {}
                }
                searchBar.addTextChangedListener(activeTextWatcher)
            }
            ViewType.SYMBOL -> {
                recyclerView.visibility = View.VISIBLE
                searchBar.visibility = View.GONE
                symButton.visibility = View.GONE
                clipboardButton.visibility = View.VISIBLE
                val adapter = getSymbolAdapter()
                recyclerView.layoutManager = GridLayoutManager(context, 10)
                recyclerView.adapter = adapter
                popupWindow?.contentView?.requestFocus()
            }
            ViewType.CLIPBOARD -> {
                searchBar.visibility = View.VISIBLE
                symButton.visibility = View.VISIBLE
                clipboardButton.visibility = View.GONE
                val history = ClipboardHistoryManager.getHistory()
                if (history.isEmpty()) {
                    emptyClipboardMessage.visibility = View.VISIBLE
                } else {
                    recyclerView.visibility = View.VISIBLE
                }

                val adapter = getClipboardAdapter()
                adapter.setHistory(history)
                recyclerView.layoutManager = LinearLayoutManager(context)
                recyclerView.adapter = adapter
                searchBar.requestFocus()
                searchBar.hint = "Search clipboard"
                activeTextWatcher = object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        adapter.filter.filter(s)
                    }
                    override fun afterTextChanged(s: Editable?) {}
                }
                searchBar.addTextChangedListener(activeTextWatcher)
            }
        }
    }

    fun isShowing(): Boolean = popupWindow?.isShowing ?: false
}