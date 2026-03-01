package com.mafg.mafg.mafg.mafg.ahorroengastosfrustrados

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class ItemAdapter(
    private val items: MutableList<Item>,
    private val onItemClick: (Item) -> Unit
) : RecyclerView.Adapter<ItemAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.itemName)
        val countView: TextView = view.findViewById(R.id.itemCount)
        val amountView: TextView = view.findViewById(R.id.itemAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_view, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context
        
        holder.textView.text = item.name
        holder.countView.text = context.getString(R.string.item_count_format, item.count)
        holder.amountView.text = String.format(Locale.getDefault(), context.getString(R.string.savings_format), item.amount)
        
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount() = items.size

    fun getItemAt(position: Int): Item {
        return items[position]
    }

    fun updateItems(newItems: List<Item>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}