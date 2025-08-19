package io.github.rickybrent.minimal_symlayer_keyboard

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

class SpecialKeyPreference(context: Context, attrs: AttributeSet?) : Preference(context, attrs) {

    private var iconResId: Int = 0
    private var tapEntriesResId: Int = 0
    private var longPressEntriesResId: Int = 0
    private var holdEntriesResId: Int = 0
    private var tapValuesResId: Int = 0
    private var longPressValuesResId: Int = 0
    private var holdValuesResId: Int = 0

    init {
        layoutResource = R.layout.settings_widget_key

        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.SpecialKeyPreference, 0, 0)
        try {
            iconResId = a.getResourceId(R.styleable.SpecialKeyPreference_keyIcon, 0)
            tapEntriesResId = a.getResourceId(R.styleable.SpecialKeyPreference_tapEntries, 0)
            longPressEntriesResId = a.getResourceId(R.styleable.SpecialKeyPreference_longPressEntries, 0)
            holdEntriesResId = a.getResourceId(R.styleable.SpecialKeyPreference_holdEntries, 0)
            tapValuesResId = a.getResourceId(R.styleable.SpecialKeyPreference_tapValues, 0)
            longPressValuesResId = a.getResourceId(R.styleable.SpecialKeyPreference_longPressValues, 0)
            holdValuesResId = a.getResourceId(R.styleable.SpecialKeyPreference_holdValues, 0)
        } finally {
            a.recycle()
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        with(holder.itemView) {
            findViewById<ImageView>(R.id.icon_special_key).setImageResource(iconResId)

            setupSpinner(findViewById(R.id.spinner_tap), tapEntriesResId, tapValuesResId, "${key}_tap")
            setupSpinner(findViewById(R.id.spinner_long_press), longPressEntriesResId, longPressValuesResId, "${key}_long_press")
            setupSpinner(findViewById(R.id.spinner_hold), holdEntriesResId, holdValuesResId, "${key}_hold")
        }
    }

    private fun setupSpinner(spinner: Spinner, entriesResId: Int, valuesResId: Int, prefKey: String) {
        if (entriesResId == 0) return
        val entries = context.resources.getStringArray(entriesResId)

        val adapter = SpecialKeySpinnerAdapter(context, entries.toList())
        spinner.adapter = adapter

        // Set the bordered background for the dropdown
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            spinner.setPopupBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.widget_eink_spinner_dropdown_border))
        }

        val values = context.resources.getStringArray(valuesResId)
        val currentValue = sharedPreferences?.getString(prefKey, values.firstOrNull())
        val currentIndex = values.indexOf(currentValue)
        if (currentIndex != -1) {
            spinner.setSelection(currentIndex)
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                sharedPreferences?.edit()?.putString(prefKey, values[position])?.apply()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
}