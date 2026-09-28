package fr.crntech.babyphone.platform

import android.content.Context
import android.os.BatteryManager
import fr.crntech.babyphone.client.platform.Battery
import fr.crntech.babyphone.client.platform.BatteryState

class AndroidBattery(context: Context) : Battery {
    private val manager = requireNotNull(context.getSystemService(BatteryManager::class.java))

    override fun read() = BatteryState(manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY), manager.isCharging)
}
