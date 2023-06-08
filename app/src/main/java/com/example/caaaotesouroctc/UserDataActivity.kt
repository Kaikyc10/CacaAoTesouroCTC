package com.example.caaaotesouroctc

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserDataActivity : AppCompatActivity() {
    private lateinit var ra: String
    private lateinit var photoImageView: ImageView
    private lateinit var raTextView: TextView
    private lateinit var locationTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_data)

        ra = intent.getStringExtra("ra") ?: ""
        photoImageView = findViewById(R.id.photoImageView)
        raTextView = findViewById(R.id.raTextView)
        locationTextView = findViewById(R.id.locationTextView)

        getUserData()
    }

    private fun getUserData() {
        val apiService = ApiClient.apiService
        val call = apiService.getData(ra)
        call.enqueue(object : Callback<GetData> {
            override fun onResponse(call: Call<GetData>, response: Response<GetData>) {
                if (response.isSuccessful) {
                    val userData = response.body()
                    if (userData != null) {
                        displayUserData(userData)
                    } else {
                        displayErrorMessage()
                    }
                } else {
                    displayErrorMessage()
                }
            }

            override fun onFailure(call: Call<GetData>, t: Throwable) {
                displayErrorMessage()
            }
        })
    }

    private fun displayUserData(userData: GetData) {
        raTextView.text = "RA: ${userData.ra}"
        locationTextView.text = "Latitude: ${userData.latitude}, Longitude: ${userData.longitude}"

        Glide.with(this)
            .load(userData.photo)
            .into(photoImageView)
    }

    private fun displayErrorMessage() {
        raTextView.text = "Dados não encontrados"
        locationTextView.text = ""
        photoImageView.setImageResource(R.drawable.ic_error_placeholder)
    }

    companion object {
        const val EXTRA_USER_DATA = "extra_user_data"
    }
}
