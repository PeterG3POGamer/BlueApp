package app.serlanventas.mobile.ui.Services

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import java.net.NetworkInterface
import java.util.Collections

object getAddressMacDivice {

    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    fun getMacAddress(context: Context): String {
        var macAddress = ""
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wInfo = wifiManager.connectionInfo
        macAddress = wInfo.macAddress

        if (macAddress == null || macAddress == "02:00:00:00:00:00") {
            try {
                val all = Collections.list(NetworkInterface.getNetworkInterfaces())
                for (nif in all) {
                    if (!nif.name.equals("wlan0", ignoreCase = true)) continue

                    val macBytes = nif.hardwareAddress ?: return ""

                    val res1 = StringBuilder()
                    for (b in macBytes) {
                        res1.append(String.format("%02X:", b))
                    }

                    if (res1.isNotEmpty()) {
                        res1.deleteCharAt(res1.length - 1)
                    }
                    macAddress = res1.toString()
                }
            } catch (ex: Exception) {
                // Manejar la excepción según sea necesario
            }
        }
        return macAddress
    }

    fun getDeviceModel(): String {
        return Build.MODEL
    }

    fun getDeviceManufacturer(): String {
        return Build.MANUFACTURER + " " + getDeviceModel()
    }

    @SuppressLint("HardwareIds")
    fun getDeviceSerialNumber(): String {
        return Build.getSerial()
    }
}
