package com.example.caaaotesouroctc

import android.service.autofill.UserData
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

// Classe de modelo para representar os dados enviados via POST
data class PostData(
    @SerializedName("ra") val ra: String,
    @SerializedName("photo") val photo: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)

// Classe de modelo para representar os dados recebidos via GET
data class GetData(
    @SerializedName("ra") val ra: String,
    @SerializedName("photo") val photo: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)

// Interface do ApiService com os métodos de requisição
interface ApiService {
    @GET("user-data/{ra}")
    fun getUserData(@Path("ra") ra: String): Call<UserData>

    @POST("post-data")
    fun sendPostData(@Body postData: PostData): Call<Void>

    @POST("data")
    fun postData(@Body data: PostData): Call<Void>

    @GET("data/{ra}")
    fun getData(@Path("ra") ra: String): Call<GetData>
}

// Objeto singleton responsável pela criação do Retrofit e do ApiService
object ApiClient {
    private const val BASE_URL = "https://restful-chiquetocaio.b4a.run/"

    val apiService: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(ApiService::class.java)
    }
}
