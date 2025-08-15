package io.github.oin.titanpocketkeyboard

import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class SymbolKey(val keyCode: Int)

class SymbolAdapter(
    private val service: InputMethodService,
    private val onKeyPress: (Int) -> Unit
) : RecyclerView.Adapter<SymbolAdapter.SymbolViewHolder>() {
    // TODO: The titan will need a different layout.
    private val symbolKeys: List<SymbolKey> = listOf(
        SymbolKey(KeyEvent.KEYCODE_Q), SymbolKey(KeyEvent.KEYCODE_W), SymbolKey(KeyEvent.KEYCODE_E),
        SymbolKey(KeyEvent.KEYCODE_R), SymbolKey(KeyEvent.KEYCODE_T), SymbolKey(KeyEvent.KEYCODE_Y),
        SymbolKey(KeyEvent.KEYCODE_U), SymbolKey(KeyEvent.KEYCODE_I), SymbolKey(KeyEvent.KEYCODE_O),
        SymbolKey(KeyEvent.KEYCODE_P),

        SymbolKey(KeyEvent.KEYCODE_A), SymbolKey(KeyEvent.KEYCODE_S), SymbolKey(KeyEvent.KEYCODE_D),
        SymbolKey(KeyEvent.KEYCODE_F), SymbolKey(KeyEvent.KEYCODE_G), SymbolKey(KeyEvent.KEYCODE_H),
        SymbolKey(KeyEvent.KEYCODE_J), SymbolKey(KeyEvent.KEYCODE_K), SymbolKey(KeyEvent.KEYCODE_L),
        SymbolKey(KeyEvent.KEYCODE_ENTER),

        SymbolKey(KeyEvent.KEYCODE_SHIFT_LEFT), SymbolKey(KeyEvent.KEYCODE_Z), SymbolKey(KeyEvent.KEYCODE_X),
        SymbolKey(KeyEvent.KEYCODE_C), SymbolKey(KeyEvent.KEYCODE_V), SymbolKey(KeyEvent.KEYCODE_B),
        SymbolKey(KeyEvent.KEYCODE_N), SymbolKey(KeyEvent.KEYCODE_M), SymbolKey(KeyEvent.KEYCODE_PERIOD),
        SymbolKey(KeyEvent.KEYCODE_DEL)
    )

    class SymbolViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.symbol_key_text_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SymbolViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.picker_item_symbol, parent, false)
        return SymbolViewHolder(view)
    }

    override fun onBindViewHolder(holder: SymbolViewHolder, position: Int) {
        val key = symbolKeys[position]
        // Use the centralized mapping logic to get the display text
        holder.textView.text = SymKeyMappings.getSymKeyDisplay(key.keyCode, service.deviceType)
        holder.itemView.setOnClickListener {
            onKeyPress(key.keyCode)
        }
    }

    override fun getItemCount(): Int = symbolKeys.size
}