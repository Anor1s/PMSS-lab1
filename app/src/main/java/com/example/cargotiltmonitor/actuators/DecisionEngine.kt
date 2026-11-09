package com.example.cargotiltmonitor.actuators

enum class Decision { ACTIVATE, RELEASE, KEEP }

class DecisionEngine(
    private val threshold: Float = ActuatorConfig.TILT_THRESHOLD_DEG,
    private val hysteresis: Float = ActuatorConfig.HYSTERESIS_DEG
) {
    fun decide(tilt: Float, active: Boolean): Decision = when {
        !active && tilt >= threshold -> Decision.ACTIVATE
        active && tilt < threshold - hysteresis -> Decision.RELEASE
        else -> Decision.KEEP
    }
}