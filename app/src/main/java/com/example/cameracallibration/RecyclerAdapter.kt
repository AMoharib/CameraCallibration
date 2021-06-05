package com.example.cameracallibration

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.cameracallibration.extensions.inflate
import com.example.cameracallibration.models.Item

class RecyclerAdapter(private val items: ArrayList<Item>): RecyclerView.Adapter<ItemVH>() {
    var onItemDeleted: ((Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemVH {
        val inflateView = parent.inflate(R.layout.image_item, false)
        return ItemVH(inflateView, onItemDeleted)
    }

    override fun onBindViewHolder(holder: ItemVH, position: Int) {
        val item = items[position]
        holder.bindView(item, position)
    }

    override fun getItemCount(): Int = items.size
}