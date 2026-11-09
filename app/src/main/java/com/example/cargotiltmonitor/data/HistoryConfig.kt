package com.example.cargotiltmonitor.data

object HistoryConfig {
    const val CHART_POINTS = 50         // скільки останніх записів показувати на графіку
    const val CHART_Y_MAX = 60f         // верхня межа осі Y (градуси)
    const val WRITE_INTERVAL_MS = 1000L // писати в БД раз на секунду
    const val WRITE_DELTA_DEG = 2f      // або раніше, якщо кут змінився на 2° і більше
    const val WRITE_MIN_GAP_MS = 200L   // але не частіше, ніж раз на 200 мс
    const val MAX_RECORDS = 2000        // зберігати не більше N останніх записів
    const val TRIM_EVERY = 100          // чистити старі записи кожні 100 вставок
}