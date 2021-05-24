package com.example.cameracallibration

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.github.piasy.biv.BigImageViewer
import com.github.piasy.biv.loader.glide.GlideImageLoader
import com.github.piasy.biv.view.BigImageView


class ImageViewer : AppCompatActivity() {

    private lateinit var bigImageView: BigImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BigImageViewer.initialize(GlideImageLoader.with(applicationContext));
        setContentView(R.layout.activity_image_viewer)

        val url = intent.extras?.getString("url")
        bigImageView = findViewById(R.id.mBigImage)
        bigImageView.showImage(Uri.parse(url))
    }
}