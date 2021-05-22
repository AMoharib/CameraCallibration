package com.example.cameracallibration.models

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
class Coordinates(
    var x: Float? = null,
    var y: Float? = null,
    var z: Float? = null
) {
}