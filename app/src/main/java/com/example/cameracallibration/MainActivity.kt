package com.example.cameracallibration

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cameracallibration.models.Item
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase


class MainActivity : AppCompatActivity() {

    private lateinit var addItemBtn: FloatingActionButton

    private lateinit var recyclerView: RecyclerView
    private lateinit var linearLayout: LinearLayoutManager

    private lateinit var auth: FirebaseAuth
    private lateinit var items: ArrayList<Item>

    private lateinit var db: FirebaseFirestore



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        db = Firebase.firestore

        addItemBtn = findViewById(R.id.addItemBtn)
        recyclerView = findViewById(R.id.recyclerView)
        linearLayout = LinearLayoutManager(this)

        recyclerView.layoutManager = linearLayout

        addItemBtn.setOnClickListener { startActivity(Intent(this, CameraActivity::class.java)) }
    }

    private fun getDataFromDB() {
        items = ArrayList()
        db.collection("items").get().addOnSuccessListener { result ->
            for(document in result){
                Log.d("MainActivity", "${document.id} => ${document.data}")
                items.add(document.toObject<Item>())
            }

            recyclerView.adapter = RecyclerAdapter(items)
        }.addOnFailureListener {

        }
    }

    override fun onStart() {
        super.onStart()
        getDataFromDB()
    }


}