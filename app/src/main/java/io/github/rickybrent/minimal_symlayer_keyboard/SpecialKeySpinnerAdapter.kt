package io.github.rickybrent.minimal_symlayer_keyboard

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class SpecialKeySpinnerAdapter(context: Context, objects: List<String>) :
    ArrayAdapter<String>(context, R.layout.settings_widget_spinner_item_collapsed, objects) {

    init {
        setDropDownViewResource(R.layout.settings_widget_spinner_item_dropdown)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent) as TextView
        val fullText = getItem(position)
        view.text = fullText?.split(" ")?.firstOrNull() ?: ""
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return super.getDropDownView(position, convertView, parent)
    }
}