package com.example.cargotiltmonitor.repository

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

// JSON-модель для хмари
data class SensorPayload(
    val deviceId: String,
    val sensorType: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val tilt: Float,
    val timestamp: Long
)

interface CloudApi {
    @POST
    suspend fun upload(@Url url: String, @Body payload: SensorPayload)
}

object CloudClient {
    val api: CloudApi by lazy {
        val http = OkHttpClient.Builder()
            .connectTimeout(CloudConfig.TIMEOUT_S, TimeUnit.SECONDS)
            .readTimeout(CloudConfig.TIMEOUT_S, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .build()
        Retrofit.Builder()
            .baseUrl(CloudConfig.BASE_URL)
            .client(http)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CloudApi::class.java)
    }
}