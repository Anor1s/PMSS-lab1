package com.example.cargotiltmonitor.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

class HistoryRepository(private val dao: SensorDao) {

    private val inserted = AtomicInteger()

    // Від старих до нових: так зручно малювати графік зліва направо
    fun observeLast(n: Int): Flow<List<SensorRecord>> = dao.observeLast(n).map { it.asReversed() }

    // БД — не в головному потоці
    suspend fun add(record: SensorRecord) = withContext(Dispatchers.IO) {
        dao.insert(record)
        if (inserted.incrementAndGet() % HistoryConfig.TRIM_EVERY == 0) dao.trim(HistoryConfig.MAX_RECORDS)
    }

    suspend fun clear() = withContext(Dispatchers.IO) { dao.clear() }
}

// Один репозиторій історії на весь додаток
object HistoryRepositories {
    @Volatile private var instance: HistoryRepository? = null

    fun get(context: Context): HistoryRepository = instance ?: synchronized(this) {
        instance ?: HistoryRepository(AppDatabase.getInstance(context).sensorDao()).also { instance = it }
    }
}