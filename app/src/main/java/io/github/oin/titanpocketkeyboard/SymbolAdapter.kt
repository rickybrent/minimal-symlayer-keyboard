package io.github.oin.titanpocketkeyboard

import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class SymbolAdapter(
    private val service: InputMethodService,
    private val onKeyPress: (Int) -> Unit
) : RecyclerView.Adapter<SymbolAdapter.SymbolViewHolder>() {

    private val symbolKeys: Map<InputMethodService.DeviceType, List<Int>> = mapOf(
        InputMethodService.DeviceType.TITAN to listOf(
            KeyEvent.KEYCODE_Q, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_E,
            KeyEvent.KEYCODE_R, KeyEvent.KEYCODE_T, KeyEvent.KEYCODE_Y,
            KeyEvent.KEYCODE_U, KeyEvent.KEYCODE_I, KeyEvent.KEYCODE_O,
            KeyEvent.KEYCODE_P,

            KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_D,
            KeyEvent.KEYCODE_F, KeyEvent.KEYCODE_G, KeyEvent.KEYCODE_H,
            KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_L,
            KeyEvent.KEYCODE_DEL,

            KeyEvent.KEYCODE_Z, KeyEvent.KEYCODE_X, KeyEvent.KEYCODE_C,
            KeyEvent.KEYCODE_V, KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_B, KeyEvent.KEYCODE_N, KeyEvent.KEYCODE_M,
            KeyEvent.KEYCODE_ENTER
        ),
        InputMethodService.DeviceType.MP01 to listOf(
            KeyEvent.KEYCODE_Q, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_E,
            KeyEvent.KEYCODE_R, KeyEvent.KEYCODE_T, KeyEvent.KEYCODE_Y,
            KeyEvent.KEYCODE_U, KeyEvent.KEYCODE_I, KeyEvent.KEYCODE_O,
            KeyEvent.KEYCODE_P,

            KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_D,
            KeyEvent.KEYCODE_F, KeyEvent.KEYCODE_G, KeyEvent.KEYCODE_H,
            KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_L,
            KeyEvent.KEYCODE_DEL,

            KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_Z, KeyEvent.KEYCODE_X,
            KeyEvent.KEYCODE_C, KeyEvent.KEYCODE_V, KeyEvent.KEYCODE_B,
            KeyEvent.KEYCODE_N, KeyEvent.KEYCODE_M, KeyEvent.KEYCODE_PERIOD,
            KeyEvent.KEYCODE_ENTER,

            0, KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_PICTSYMBOLS,
            KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_SYM, KeyEvent.KEYCODE_SHIFT_RIGHT
        )
    )

    class SymbolViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.symbol_key_text_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SymbolViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.picker_item_symbol, parent, false)
        return SymbolViewHolder(view)
    }

    override fun onBindViewHolder(holder: SymbolViewHolder, position: Int) {
        val keyCode = symbolKeys[service.deviceType]?.get(position) ?: 0
        if (keyCode == 0) {
            holder.itemView.alpha = 0f // Not a key, a placeholder.
            return
        }
        holder.itemView.alpha = 1f
        // Use the centralized mapping logic to get the display text
        holder.textView.text = SymKeyMappings.getSymKeyDisplay(keyCode, service.deviceType)
        holder.itemView.setOnClickListener {
            onKeyPress(keyCode)
        }
    }

    override fun getItemCount(): Int = symbolKeys[service.deviceType]?.size ?:0
}