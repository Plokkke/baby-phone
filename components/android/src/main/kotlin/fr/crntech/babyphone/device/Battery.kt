package fr.crntech.babyphone.device

import android.content.Context
import android.os.BatteryManager

data class BatteryState(val percent: Int, val charging: Boolean)

class Battery(context: Context) {
    private val manager = requireNotNull(context.getSystemService(BatteryManager::class.java))

    fun read() = BatteryState(manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY), manager.isCharging)
}
