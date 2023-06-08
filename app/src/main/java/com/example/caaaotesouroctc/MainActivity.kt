package com.example.caaaotesouroctc

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.service.autofill.UserData
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.caaaotesouroctc.databinding.ActivityMainBinding
import com.google.android.gms.location.*
import com.google.zxing.integration.android.IntentIntegrator
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var currentLocation: Location

    private val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://restful-chiquetocaio.b4a.run/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private val validRAList = listOf("123", "456", "789") // Lista de RA válidos

    private var currentPhotoBitmap: Bitmap? = null
    private var currentRA: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        binding.qrCodeButton.setOnClickListener {
            scanQrCode()
        }

        binding.photoButton.setOnClickListener {
            takePhoto()
        }

        binding.postButton.setOnClickListener {
            onPostButtonClicked()
        }

        binding.getButton.setOnClickListener {
            onGetButtonClicked()
        }

        requestLocationPermission()
        createLocationCallback()
    }

    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    handleLocationResult(location)
                }
            }
        }
    }

    private fun handleLocationResult(location: Location) {
        if (binding.qrCodeTextView.visibility == View.VISIBLE) {
            val latitude = location.latitude
            val longitude = location.longitude

            // Faça algo com as coordenadas de latitude e longitude
            // Por exemplo, exiba as coordenadas em um TextView centralizado
            binding.locationTextView.text = "Latitude: $latitude\nLongitude: $longitude"
            binding.locationTextView.visibility = View.VISIBLE

            // Outras operações com os dados de localização
            // ...
        }
    }

    private fun scanQrCode() {
        IntentIntegrator(this).initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IntentIntegrator.REQUEST_CODE && resultCode == RESULT_OK) {
            val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
            val qrCodeMessage = result.contents // Obtenha a mensagem do QR code aqui
            binding.qrCodeTextView.text = qrCodeMessage
            binding.qrCodeTextView.visibility = View.VISIBLE
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val photoUri: Uri? = data?.data
            val photoBitmap: Bitmap? = photoUri?.let { getBitmapFromUri(it) }
            if (photoBitmap != null) {
                currentPhotoBitmap = photoBitmap
                Toast.makeText(this, "Foto capturada com sucesso.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Falha ao capturar a foto.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getBase64EncodedPhoto(photoBitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        photoBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        val photoBytes = outputStream.toByteArray()
        val base64Encoded = Base64.encodeToString(photoBytes, Base64.DEFAULT)
        return base64Encoded
    }

    private fun takePhoto() {
        val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePhotoIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePhotoIntent, REQUEST_IMAGE_CAPTURE)
        } else {
            Toast.makeText(this, "Não foi possível abrir o aplicativo de câmera.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onPostButtonClicked() {
        val ra = binding.raEditText.text.toString()
        val locationData = getLocationData()

        if (ra.isNotEmpty() && currentPhotoBitmap != null && locationData != null) {
            if (validRAList.contains(ra)) {
                currentRA = ra
                val photoData = getBase64EncodedPhoto(currentPhotoBitmap!!)
                val postData = PostData(ra, photoData, locationData.latitude, locationData.longitude)
                apiService.sendPostData(postData).enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@MainActivity, "Dados enviados com sucesso.", Toast.LENGTH_SHORT).show()
                            clearFormData()
                            finish() // Fechar o aplicativo após o envio dos dados
                        } else {
                            Toast.makeText(this@MainActivity, "Falha ao enviar os dados.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Log.e(TAG, "Falha ao enviar os dados: ${t.message}")
                        Toast.makeText(this@MainActivity, "Falha ao enviar os dados.", Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                Toast.makeText(this, "RA inválido.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Preencha todos os campos corretamente.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onGetButtonClicked() {
        val ra = binding.raDataEditText.text.toString()
        if (ra.isNotEmpty()) {
            fetchUserData(ra)
        } else {
            Toast.makeText(this, "Digite um RA válido.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearFormData() {
        binding.raEditText.text.clear()
        binding.raDataEditText.text.clear()
        binding.qrCodeTextView.text = ""
        binding.qrCodeTextView.visibility = View.GONE
        binding.locationTextView.text = ""
        binding.locationTextView.visibility = View.GONE
        currentPhotoBitmap = null
        currentRA = ""
    }

    private fun fetchUserData(ra: String) {
        apiService.getUserData(ra).enqueue(object : Callback<UserData> {
            override fun onResponse(call: Call<UserData>, response: Response<UserData>) {
                if (response.isSuccessful) {
                    val userData = response.body()
                    if (userData != null) {
                        navigateToUserData(userData)
                    } else {
                        Toast.makeText(this@MainActivity, "Nenhum dado encontrado para o RA fornecido.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Falha ao obter os dados do usuário.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserData>, t: Throwable) {
                Log.e(TAG, "Falha ao obter os dados do usuário: ${t.message}")
                Toast.makeText(this@MainActivity, "Falha ao obter os dados do usuário.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun navigateToUserData(userData: UserData) {
        val intent = Intent(this, UserDataActivity::class.java)
        intent.putExtra(UserDataActivity.EXTRA_USER_DATA, userData)
        startActivity(intent)
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            startLocationUpdates()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null
        )
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun getLocationData(): Location? {
        return if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            currentLocation
        } else {
            null
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
            } else {
                Toast.makeText(this, "Permissão de localização negada.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val LOCATION_PERMISSION_REQUEST_CODE = 2
        private const val REQUEST_CODE_QR_SCAN = 123
        private const val TAG = "MainActivity"

        private val validRAList = listOf("123", "456", "789") // Lista de RA válidos
    }
}

