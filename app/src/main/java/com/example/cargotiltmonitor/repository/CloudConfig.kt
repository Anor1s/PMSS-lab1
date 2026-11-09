package com.example.cargotiltmonitor.repository

object CloudConfig {

    const val URL = "https://webhook.site/3ed7ac9c-c9bd-4959-8982-74f78659695e"
    const val BASE_URL = "https://webhook.site/"

    const val DEVICE_ID = "cargo-tilt-01"
    const val SENSOR_TYPE = "accelerometer"

    const val SEND_INTERVAL_MS = 20000L // як часто слати дані (мс): раз на 20 с
    const val SEND_DELTA_DEG = 5f       // слати раніше, якщо кут змінився на 5° і більше
    const val MIN_GAP_MS = 5000L        // мінімальна пауза між відправками (мс), навіть при різкому нахилі
    const val TIMEOUT_S = 10L           // скільки секунд чекати відповідь сервера, потім помилка
}