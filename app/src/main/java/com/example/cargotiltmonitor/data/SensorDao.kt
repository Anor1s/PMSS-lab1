package com.example.cargotiltmonitor.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorDao {

    @Insert
    suspend fun insert(record: SensorRecord)

    // Останні N записів (новіші першими); Flow оновлюється автоматично при кожній вставці
    @Query("SELECT * FROM sensor_record ORDER BY timestamp DESC LIMIT :n")
    fun observeLast(n: Int): Flow<List<SensorRecord>>

    // Залишає лише keep останніх записів
    @Query("DELETE FROM sensor_record WHERE id NOT IN (SELECT id FROM sensor_record ORDER BY timestamp DESC LIMIT :keep)")
    suspend fun trim(keep: Int)

    @Query("DELETE FROM sensor_record")
    suspend fun clear()
}