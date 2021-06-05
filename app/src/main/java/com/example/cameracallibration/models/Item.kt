package com.example.cameracallibration.models

import android.location.Location
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
class Item(
    var coordinates: Coordinates? = null,
    var imageUri: String? = null,
    var userId: String? = null,
    var unitName: String? = null,
    var quantity: String? = null,
    var airplane: String? = null,
    var location: ItemLocation? = null,
    var time: Long? = null,
    var documentId: String? = null
) {
}