package com.example.cargotiltmonitor.helpers

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration.Companion.milliseconds

class MockBleScanner : IBleScanner {
    override fun scan(): Flow<BleDevice> = flow {
        delay(BleProfile.SCAN_DELAY_MS.milliseconds)
        for (d in BleProfile.MOCK_DEVICES) {
            emit(d)
            delay(BleProfile.SCAN_STEP_MS.milliseconds)
        }
    }
}