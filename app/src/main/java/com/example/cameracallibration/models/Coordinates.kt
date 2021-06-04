package com.example.cameracallibration.models

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
class Coordinates(
    var azimuth: Float? = null,
    var roll: Float? = null,
    var pitch: Float? = null
) {
}