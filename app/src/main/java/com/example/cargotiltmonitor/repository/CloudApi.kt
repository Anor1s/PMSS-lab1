package com.example.cargotiltmonitor.repository

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

data class SensorPayload(
    val deviceId: String,
    val sensorType: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val tilt: Float,
    val timestamp: Long
)

// Команда з хмари: "on" / "off" для кожного пристрою
data class RemoteCommand(
    val lamp: String? = null,
    val siren: String? = null,
    val flashlight: String? = null
)

interface CloudApi {
    @POST
    suspend fun upload(@Url url: String, @Body payload: SensorPayload)

    @GET
    suspend fun fetchCommand(@Url url: String): RemoteCommand
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