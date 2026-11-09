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
            throw e // скасування корутини не "ковтаємо"
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Опитування (polling) каналу команд
    suspend fun fetchCommand(): Result<RemoteCommand> = withContext(Dispatchers.IO) {
        try {
            Result.success(api.fetchCommand(CloudConfig.COMMAND_URL))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Один репозиторій на весь додаток
object AppRepositories {
    val sensors: SensorRepository by lazy { SensorRepository(CloudClient.api) }
}