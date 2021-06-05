package com.example.cameracallibration

import android.content.Intent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cameracallibration.models.Item
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.*

class ItemVH(itemView: View, val onItemDeleted: ((Int) -> Unit)?) : RecyclerView.ViewHolder(itemView) {
    private var coordinates: TextView = itemView.findViewById(R.id.coordinates)
    private var image: ImageView = itemView.findViewById(R.id.image)
    private var deleteBtn: ImageView = itemView.findViewById(R.id.deleteBtn)
    private lateinit var item: Item
    private var pos: Int? = null
    private var firestore: FirebaseFirestore

    init {
        itemView.setOnClickListener {
            val intent = Intent(itemView.context, ImageViewer::class.java)
            intent.putExtra("url", item.imageUri)
            itemView.context.startActivity(intent)
        }

        deleteBtn.setOnClickListener {
            deleteItem()
        }

        firestore = Firebase.firestore

    }

    fun bindView(item: Item, position: Int) {
        this.item = item
        this.pos = position
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
                SimpleDateFormat("dd-MMM-yyyy hh:mm:ss aa").format(item.time)
            }"
        Picasso.get().load(item.imageUri).into(image)
    }

    fun deleteItem() {
        item.documentId?.let {
            firestore.collection("items")
                .document(it)
                .delete()
                .addOnSuccessListener {
                    this.pos?.let { it1 -> this.onItemDeleted?.invoke(it1) }
                }
                .addOnFailureListener { e ->  }
        }
    }
}