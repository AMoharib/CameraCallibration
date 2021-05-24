package com.example.cameracallibration

import android.content.Intent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cameracallibration.models.Item
import com.squareup.picasso.Picasso

class ItemVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private var coordinates: TextView = itemView.findViewById(R.id.coordinates)
    private var image: ImageView = itemView.findViewById(R.id.image)
    private lateinit var item: Item

    init {
        itemView.setOnClickListener {
            val intent = Intent(itemView.context, ImageViewer::class.java)
            intent.putExtra("url", item.imageUri)
            itemView.context.startActivity(intent)
        }
    }

    fun bindView(item: Item) {
        this.item = item
        coordinates.text = "X: ${
            item.coordinates?.x?.toDouble()?.let { Math.toDegrees(it).toInt() }
        }\t\t\t Y: ${
            item.coordinates?.y?.toDouble()?.let { Math.toDegrees(it).toInt() }
        }\t\t\t Z: ${
            item.coordinates?.z?.toDouble()?.let { ((Math.toDegrees(it) + 360) % 360).toInt() }
        }\n Lat: ${
            item.location?.lat
        }\t\t\t Long: ${
            item.location?.long
        }\n Type: ${
            item.airplane?.type
        }\t\t\t Model: ${
            item.airplane?.model
        }"
        Picasso.get().load(item.imageUri).into(image)
    }
}