package com.woleapp.netpos.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.content.Intent
import android.os.BatteryManager
import android.widget.Toast
import com.netpluspay.kozenlib.KozenLib
import com.woleapp.netpos.model.BatteryEvents
import com.woleapp.netpos.model.MqttEvent
import com.woleapp.netpos.model.MqttEvents
import com.woleapp.netpos.model.User
import com.woleapp.netpos.mqtt.MqttHelper
import com.woleapp.netpos.util.Singletons
import timber.log.Timber


class BatteryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, batteryStatus: Intent?) {
        val user: User? = Singletons.getCurrentlyLoggedInUser()
        val savedConfigurationData = Singletons.getSavedConfigurationData()
        user?.let {
            var event: MqttEvent? = MqttEvent(
                it.netplus_id!!,
                it.business_name!!,
                savedConfigurationData.terminalId,
                KozenLib.getDeviceSerial()
            ).apply {
                status = "SUCCESS"
                code = "00"
                timestamp = System.currentTimeMillis()
                event = MqttEvents.BATTERY_EVENTS.event
            }
            val batteryData: BatteryEvents? = when (batteryStatus?.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    Timber.e("Power Connected")
                    Toast.makeText(context, "Power Connected", Toast.LENGTH_SHORT).show()
                    BatteryEvents(batteryPercentage(context!!), "Charging")
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    Timber.e("Power disconnected")
                    Toast.makeText(context, "Power disconnected", Toast.LENGTH_SHORT).show()
                    BatteryEvents(batteryPercentage(context!!), "Discharging")
                }
                Intent.ACTION_BATTERY_LOW -> {
                    BatteryEvents(batteryPercentage(context!!), "Battery Low")
                }
                Intent.ACTION_BATTERY_OKAY -> {
                    BatteryEvents(batteryPercentage(context!!), "Battery Okay")
                }
                else -> {
                    event = null
                    null
                }
            }
            event?.apply {
                data = batteryData
                MqttHelper.sendPayload(this)
            }
        }
    }

    private fun batteryPercentage(context: Context): Int =
        (context.getSystemService(BATTERY_SERVICE) as BatteryManager).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

}