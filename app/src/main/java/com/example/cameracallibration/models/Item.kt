package com.example.cameracallibration.models

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
class Item(
    var coordinates: Coordinates? = null,
    var imageUri: String? = null,
    var userId: String? = null
) {
}