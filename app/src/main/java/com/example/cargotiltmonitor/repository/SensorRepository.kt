package com.example.cargotiltmonitor.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SensorRepository(private val api: CloudApi) {

    // Мережа — не в головному потоці; результат Result, щоб ViewModel знала, чи вдалося
    suspend fun save(reading: SensorReading): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            api.upload(CloudConfig.URL, reading.toPayload())
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

object AppRepositories {
    val sensors: SensorRepository by lazy { SensorRepository(CloudClient.api) }
}