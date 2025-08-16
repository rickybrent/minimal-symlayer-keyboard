package io.github.oin.titanpocketkeyboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ClipboardAdapter(
    private val onItemSelected: (String) -> Unit
) : RecyclerView.Adapter<ClipboardAdapter.ClipboardViewHolder>(), Filterable {

    private var history: List<String> = listOf()
    private var filteredHistory: List<String> = listOf()

    fun setHistory(history: List<String>) {
        this.history = history
        this.filteredHistory = history
        notifyDataSetChanged()
    }

    class ClipboardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClipboardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ClipboardViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClipboardViewHolder, position: Int) {
        val item = filteredHistory[position]
        holder.textView.text = item
        holder.itemView.setOnClickListener {
            onItemSelected(item)
        }
    }

    override fun getItemCount(): Int = filteredHistory.size

    fun selectFirstItem() {
        if (filteredHistory.isNotEmpty()) {
            onItemSelected(filteredHistory[0])
        }
    }
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                val query = constraint?.toString()?.lowercase()
                results.values = if (query.isNullOrEmpty()) {
                    history
                } else {
                    history.filter {
                        it.lowercase().contains(query)
                    }
                }
                return results
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredHistory = results?.values as? List<String> ?: emptyList()
                notifyDataSetChanged()
            }
        }
    }
}