package com.example.foodorderapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private val TAG = "FirebaseTest"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        testFirebaseConnection()
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
        }

    private fun testFirebaseConnection(){
        val db=FirebaseFirestore.getInstance()

        val testData = hashMapOf(
            "message" to "Hello Firebase!",
            "timestamp" to System.currentTimeMillis(),
            "app" to "FoodOrderApp"
        )

        db.collection("test")
            .add(testData)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Document berhasil ditambahkan dengan ID: ${documentReference.id}")
                Toast.makeText(this, "Firebase Connected!!", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error menambahkan document", e)
                Toast.makeText(this, "Firebase Error:  ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

}