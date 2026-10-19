package com.example.cargotiltmonitor.helpers

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BleHub {
    val connector: IBleConnector = MockBleConnector()
    val scanner: IBleScanner = MockBleScanner()
}

object BleLog {
    private const val TAG = "BLE"
    private val fmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    @Synchronized
    fun d(msg: String): String {
        Log.d(TAG, msg)
        return "${fmt.format(Date())}  $msg"
    }
}