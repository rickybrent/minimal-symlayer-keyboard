package io.github.rickybrent.minimal_symlayer_keyboard

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
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
    private var clipboardAdapter: ClipboardHistoryAdapter? = null
    private var popupShownTime: Long = 0
    private var initialPressComplete = false
    private var activeTextWatcher: TextWatcher? = null
    private enum class PickerMode { POPUP, INLINE }
    private var currentMode = PickerMode.POPUP
    private var currentView: ViewType = ViewType.EMOJI

    private var inlineViewContainer: FrameLayout? = null
    private val pickerView: View

    private lateinit var contentArea: FrameLayout
    private lateinit var titleArea: TextView
    private lateinit var searchBar: EditText
    private lateinit var emojiButton: ImageButton
    private lateinit var emojiCloseButton: ImageButton
    private lateinit var symButton: ImageButton
    private lateinit var symCloseButton: ImageButton
    private lateinit var clipboardButton: ImageButton
    private lateinit var emptyClipboardMessage: TextView
    private lateinit var recyclerView: RecyclerView

    enum class ViewType {
        EMOJI, SYMBOL, CLIPBOARD
    }

    init {
        pickerView = View.inflate(service, R.layout.picker_container, null)
    }

    fun setPickerMode(useInline: Boolean) {
        currentMode = if (useInline) PickerMode.INLINE else PickerMode.POPUP
    }

    fun setInlineViewContainer(container: FrameLayout?) {
        inlineViewContainer = container
        // Ensure pickerView is not attached to a different parent
        (pickerView.parent as? ViewGroup)?.removeView(pickerView)
        inlineViewContainer?.addView(pickerView)
    }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        if (event.action == KeyEvent.ACTION_UP && keyCode == MP01_KEYCODE_EMOJI_PICKER) {
            // Prevent the key up event from the opening hotkey from immediately closing the popup.
            if (!initialPressComplete && System.currentTimeMillis() - popupShownTime < 1000) {
                initialPressComplete = true
                return true // Consume the event
            }
            if (currentView == ViewType.EMOJI)
                hide()
            else
                switchToView(ViewType.EMOJI)
            return true
        }

        if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_SYM) {
            if (currentView == ViewType.SYMBOL)
                hide()
            else
                switchToView(ViewType.SYMBOL)
            return true
        }

        // Handle 'Enter' key only if the search bar has focus
        if (keyCode == KeyEvent.KEYCODE_ENTER && searchBar.hasFocus()) {
            if (event.action == KeyEvent.ACTION_DOWN) return true // Consume down event
            if (event.action == KeyEvent.ACTION_UP) {
                // This will insert text or an emoji and dismiss the popup.
                when (currentView) {
                    ViewType.EMOJI -> getEmojiAdapter().selectFirstEmoji()
                    ViewType.CLIPBOARD -> getClipboardAdapter().selectFirstItem()
                    else -> {}
                }
                return true
            }
        }

        // Forward other key events to the search bar if it has focus
        if (searchBar.hasFocus() && event.keyCode != KeyEvent.KEYCODE_BACK) {
             if (event.action == KeyEvent.ACTION_DOWN) {
                searchBar.onKeyDown(keyCode, event)
             } else {
                searchBar.onKeyUp(keyCode, event)
             }
             return true
        } else if (currentView == ViewType.SYMBOL) {
            // Forward all input as sym key presses on the sym key view.
            return service.forceSymKeyEvent(event)
        }

        return false // Don't consume other events
    }

    private fun getEmojiAdapter(): EmojiAdapter {
        if (emojiAdapter == null) {
            emojiAdapter = EmojiAdapter(context) { emoji ->
                service.currentInputConnection?.commitText(emoji.character, 1)
                hide()
            }
        }
        return emojiAdapter!!
    }

    private fun getSymbolAdapter(): SymbolAdapter {
        if (symbolAdapter == null) {
            symbolAdapter = SymbolAdapter(service) { keyCode ->
                if (keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT) {
                    if (service.shift.get()) {
                        service.shift.reset()
                    } else {
                        service.shift.onKeyDown()
                        service.shift.onKeyUp()
                    }
                    service.updateModStateIcon()
                } else if (keyCode == KeyEvent.KEYCODE_SPACE || keyCode == KeyEvent.KEYCODE_DEL) {
                    service.sendDownUpKeyEvents(keyCode)
                } else {
                    service.onSymKey(KeyEvent(KeyEvent.ACTION_DOWN, keyCode), true)
                    service.onSymKey(KeyEvent(KeyEvent.ACTION_UP, keyCode), false)
                }
            }
        }
        return symbolAdapter!!
    }

    private fun getClipboardAdapter(): ClipboardHistoryAdapter {
        if (clipboardAdapter == null) {
            clipboardAdapter = ClipboardHistoryAdapter(context) { text ->
                service.currentInputConnection?.commitText(text, 1)
                hide()
            }
        }
        return clipboardAdapter!!
    }


    fun show(startingView: ViewType = ViewType.EMOJI) {
        initialPressComplete = false
        popupShownTime = System.currentTimeMillis()
        if (!::contentArea.isInitialized) {
            setupPickerView()
        }
        val height = (context.resources.displayMetrics.heightPixels / 2.25).toInt()
        if (currentMode == PickerMode.INLINE) {
            if (isShowing()) {
                hide()
                return
            }
            switchToView(startingView) // Default to emoji view
            inlineViewContainer?.let {
                val layoutParams = it.layoutParams
                layoutParams.height = height
                it.layoutParams = layoutParams
                it.visibility = View.VISIBLE
            }
            return
        }

        if (popupWindow == null) {
            setupPopupWindow(height)
        }

        if (popupWindow?.isShowing == true) {
            hide()
        } else {
            switchToView(startingView) // Default to emoji view
            popupWindow?.showAtLocation(service.window.window!!.decorView, Gravity.BOTTOM, 0, 0)
        }
    }

    fun hide() {
        if (currentMode == PickerMode.INLINE) {
            inlineViewContainer?.visibility = View.GONE
        } else {
            popupWindow?.dismiss()
        }
    }

    private fun setupPickerView() {
        contentArea = pickerView.findViewById(R.id.picker_content_area)
        searchBar = pickerView.findViewById(R.id.search_bar)
        titleArea = pickerView.findViewById(R.id.picker_title)
        emojiButton = pickerView.findViewById<ImageButton>(R.id.emoji_view_button)
        emojiCloseButton = pickerView.findViewById<ImageButton>(R.id.emoji_close_button)
        symButton = pickerView.findViewById(R.id.sym_button)
        symCloseButton = pickerView.findViewById(R.id.sym_close_button)
        clipboardButton = pickerView.findViewById(R.id.clipboard_button)
        emptyClipboardMessage = pickerView.findViewById(R.id.empty_clipboard_message)

        // Create and add the RecyclerView here
        recyclerView = RecyclerView(context)
        contentArea.addView(recyclerView)


        symCloseButton.setOnClickListener { hide() }
        emojiCloseButton.setOnClickListener { hide() }
        emojiButton.setOnClickListener { switchToView(ViewType.EMOJI) }
        symButton.setOnClickListener { switchToView(ViewType.SYMBOL) }
        clipboardButton.setOnClickListener { switchToView(ViewType.CLIPBOARD) }

        pickerView.isFocusableInTouchMode = false
    }

    private fun setupPopupWindow(height: Int) {
        popupWindow = PopupWindow(
            pickerView,
            FrameLayout.LayoutParams.MATCH_PARENT,
            height
        ).apply {
            // Prevent popupWindow from stealing focus during sym input; we'll forward events from the InputMethodService as needed instead.
            isFocusable = false
            isOutsideTouchable = true
        }
        popupWindow?.setOnDismissListener {
            service.clearModifiers()
        }
    }

    private fun switchToView(viewType: ViewType) {
        currentView = viewType

        recyclerView.visibility = View.GONE
        emptyClipboardMessage.visibility = View.GONE

        // Remove the searchBar watcher before switching
        activeTextWatcher?.let { searchBar.removeTextChangedListener(it) }

        symCloseButton.visibility = View.GONE
        emojiCloseButton.visibility = View.GONE
        titleArea.visibility = View.GONE
        emojiButton.visibility = View.VISIBLE
        symButton.visibility = View.VISIBLE
        searchBar.visibility = View.VISIBLE
        clipboardButton.visibility = View.VISIBLE

        when (viewType) {
            ViewType.EMOJI -> {
                emojiButton.visibility = View.GONE
                emojiCloseButton.visibility = View.VISIBLE
                recyclerView.visibility = View.VISIBLE
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
                symButton.visibility = View.GONE
                symCloseButton.visibility = View.VISIBLE
                recyclerView.visibility = View.VISIBLE
                searchBar.visibility = View.GONE
                titleArea.visibility = View.VISIBLE
                val adapter = getSymbolAdapter()
                val layoutManager = GridLayoutManager(context, 10)
                layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return adapter.getSpanSize(position)
                    }
                }
                recyclerView.layoutManager = layoutManager
                recyclerView.adapter = adapter
                popupWindow?.contentView?.requestFocus()
            }
            ViewType.CLIPBOARD -> {
                clipboardButton.visibility = View.GONE
                val adapter = getClipboardAdapter()
                adapter.refresh()
                val history = adapter.getHistory()
                if (history.isEmpty()) {
                    emptyClipboardMessage.visibility = View.VISIBLE
                } else {
                    recyclerView.visibility = View.VISIBLE
                }
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

    fun isShowing(): Boolean {
        if (currentMode == PickerMode.INLINE) {
            return inlineViewContainer?.visibility == View.VISIBLE
        }
        return popupWindow?.isShowing ?: false
    }
}