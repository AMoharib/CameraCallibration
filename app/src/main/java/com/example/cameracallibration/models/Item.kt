package com.example.cameracallibration.models

import android.location.Location
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
class Item(
    var coordinates: Coordinates? = null,
    var imageUri: String? = null,
    var userId: String? = null,
    var location: ItemLocation? = null,
    var airplane: Airplane? = null
) {
}