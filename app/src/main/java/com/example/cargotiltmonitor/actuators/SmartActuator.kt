package com.example.cargotiltmonitor.actuators

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class SmartActuator(context: Context) {

    private val tag = "Actuator"
    private val appContext = context.applicationContext
    private val vibrator: Vibrator? = createVibrator()
    private val cameraManager = appContext.getSystemService(CameraManager::class.java)

    // Перевірка апаратної можливості перед викликом коду
    private val hasFlashFeature =
        appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    private val torchId: String? = if (hasFlashFeature) findTorchId() else null
    val canFlash: Boolean get() = torchId != null

    init {
        Log.i(tag, "FEATURE_CAMERA_FLASH=$hasFlashFeature, torchId=$torchId, hasVibrator=${vibrator?.hasVibrator()}")
    }

    private fun createVibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            appContext.getSystemService(VibratorManager::class.java)?.defaultVibrator
        else legacyVibrator()

    @Suppress("DEPRECATION")
    private fun legacyVibrator(): Vibrator? =
        appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    private fun findTorchId(): String? = try {
        cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    } catch (e: CameraAccessException) {
        null
    }

    fun triggerAlarm() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) { Log.w(tag, "Vibrator: немає апаратної вібрації"); return }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            v.vibrate(VibrationEffect.createWaveform(ActuatorConfig.ALARM_PATTERN, 0))
        else legacyVibrate(v)
        Log.i(tag, "Alarm: вібрація запущена")
    }

    @Suppress("DEPRECATION")
    private fun legacyVibrate(v: Vibrator) = v.vibrate(ActuatorConfig.ALARM_PATTERN, 0)

    fun stopAlarm() {
        vibrator?.cancel()
        Log.i(tag, "Alarm: вібрація зупинена")
    }

    fun toggleFlashlight(status: Boolean): Boolean {
        val id = torchId ?: run { Log.w(tag, "Flashlight: недоступний, лише віртуальний"); return false }
        return try {
            cameraManager.setTorchMode(id, status)
            Log.i(tag, "Flashlight: setTorchMode(id=$id, $status) OK")
            true
        } catch (e: CameraAccessException) {
            Log.w(tag, "Flashlight: CameraAccessException ${e.message}")
            false
        } catch (e: IllegalArgumentException) {
            Log.w(tag, "Flashlight: IllegalArgumentException ${e.message}")
            false
        }
    }

    fun release() {
        stopAlarm()
        toggleFlashlight(false)
    }
}