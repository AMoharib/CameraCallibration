package com.example.cameracallibration

import android.content.Intent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cameracallibration.models.Item
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.*

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
        coordinates.text =
            "Azimuth: ${
                item.coordinates?.azimuth?.toDouble()
                    ?.let { ((Math.toDegrees(it) + 360) % 360).toInt() }
            }°\t\t\t Inclination: ${
                item.coordinates?.roll?.toDouble()
                    ?.let { ((Math.toDegrees(it) + 360) % 360).toInt() }
            }°\n Lat: ${
                item.location?.lat
            }\t\t\t Long: ${
                item.location?.long
            }\n Unit Name: ${
                item.unitName
            }\t\t\t Quantity: ${
                item.quantity
            }\t\t\t Type: ${
                item.airplane
            }\n Time: ${
                SimpleDateFormat("hh:mm:ss aa").format(item.time)
            }"
        Picasso.get().load(item.imageUri).into(image)
    }
}