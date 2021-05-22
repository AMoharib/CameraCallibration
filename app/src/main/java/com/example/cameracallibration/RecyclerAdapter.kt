package com.example.cameracallibration

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.cameracallibration.extensions.inflate
import com.example.cameracallibration.models.Item

class RecyclerAdapter(private val items: ArrayList<Item>): RecyclerView.Adapter<ItemVH>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemVH {
        val inflateView = parent.inflate(R.layout.image_item, false)
        return ItemVH(inflateView)
    }

    override fun onBindViewHolder(holder: ItemVH, position: Int) {
        val item = items[position]
        holder.bindView(item)
    }

    override fun getItemCount(): Int = items.size
}