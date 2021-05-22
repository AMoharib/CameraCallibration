package com.example.cameracallibration

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cameracallibration.models.Item
import com.squareup.picasso.Picasso

class ItemVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private var coordinates: TextView = itemView.findViewById(R.id.coordinates)
    private var image: ImageView = itemView.findViewById(R.id.image)

    fun bindView(item: Item) {
        coordinates.text = "X: ${
            item.coordinates?.x?.toDouble()?.let { Math.toDegrees(it).toInt() }
        }\t\t\t Y: ${
            item.coordinates?.y?.toDouble()?.let { Math.toDegrees(it).toInt() }
        }\t\t\t Z: ${
            item.coordinates?.z?.toDouble()?.let { ((Math.toDegrees(it) + 360) % 360).toInt() }
        }"
        Picasso.get().load(item.imageUri).into(image)
    }
}