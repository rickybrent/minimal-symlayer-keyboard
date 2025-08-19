package io.github.rickybrent.minimal_symlayer_keyboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ClipboardAdapter(
    private val onItemSelected: (String) -> Unit
) : RecyclerView.Adapter<ClipboardAdapter.ClipboardViewHolder>(), Filterable {

    private var fullHistory: List<Clipping> = listOf()
    private var filteredHistory: List<Clipping> = listOf()

    fun setHistory(history: List<Clipping>) {
        this.fullHistory = history
        this.filteredHistory = history
        notifyDataSetChanged()
    }

    class ClipboardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.clipboard_text)
        val pinIcon: ImageView = view.findViewById(R.id.pin_icon)
        val clearIcon: ImageView = view.findViewById(R.id.clear_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClipboardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.picker_item_clipping, parent, false)
        return ClipboardViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClipboardViewHolder, position: Int) {
        val item = filteredHistory[position]
        holder.textView.text = item.text
        holder.pinIcon.setImageResource(
            if (item.isPinned) R.drawable.ic_pin_filled else R.drawable.ic_pin_outline
        )

        holder.itemView.setOnClickListener {
            onItemSelected(item.text)
        }

        holder.pinIcon.setOnClickListener {
            ClipboardHistoryManager.togglePin(item)
            setHistory(ClipboardHistoryManager.getHistory()) // Refresh the list
        }

        holder.clearIcon.setOnClickListener {
            ClipboardHistoryManager.removeItem(item)
            setHistory(ClipboardHistoryManager.getHistory()) // Refresh the list
        }
    }

    override fun getItemCount(): Int = filteredHistory.size

    fun selectFirstItem() {
        if (filteredHistory.isNotEmpty()) {
            onItemSelected(filteredHistory[0].text)
        }
    }
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                val query = constraint?.toString()?.lowercase()
                results.values = if (query.isNullOrEmpty()) {
                    fullHistory
                } else {
                    fullHistory.filter {
                        it.text.lowercase().contains(query)
                    }
                }
                return results
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredHistory = results?.values as? List<Clipping> ?: emptyList()
                notifyDataSetChanged()
            }
        }
    }
}