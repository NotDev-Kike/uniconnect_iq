package com.example.uniconnect_iq

import android.Manifest
import android.content.Context
import android.net.wifi.WifiManager
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresPermission
import java.net.NetworkInterface


object DeviceIdentifier {

    private const val TAG = "DeviceIdentifier"


    /**
     * Obtiene la MAC del dispositivo WiFi.
     * Formato esperado por el radar:
     * aabbccddeeff
     */
    @RequiresPermission(Manifest.permission.ACCESS_WIFI_STATE)
    fun getMacAddress(context: Context): String {

        try {

            // Método 1: obtener desde interfaz wlan0
            val interfaces =
                NetworkInterface.getNetworkInterfaces()


            for (networkInterface in interfaces) {


                if (
                    networkInterface.name.equals(
                        "wlan0",
                        ignoreCase = true
                    )
                ) {


                    val macBytes =
                        networkInterface.hardwareAddress


                    if (macBytes != null) {


                        val mac =
                            macBytes.joinToString("") {

                                String.format(
                                    "%02x",
                                    it
                                )

                            }


                        if (mac.isNotEmpty()) {

                            Log.d(
                                TAG,
                                "MAC obtenida wlan0: $mac"
                            )

                            return mac.lowercase()

                        }

                    }

                }

            }



            // Método 2: WifiManager
            val wifiManager =
                context.applicationContext
                    .getSystemService(
                        Context.WIFI_SERVICE
                    ) as WifiManager



            val wifiInfo =
                wifiManager.connectionInfo



            val macWifi =
                wifiInfo.macAddress



            if (
                macWifi != null &&
                macWifi != "02:00:00:00:00:00"
            ) {


                val mac =
                    macWifi
                        .replace(":", "")
                        .lowercase()


                Log.d(
                    TAG,
                    "MAC obtenida WifiManager: $mac"
                )


                return mac

            }



        } catch (e: Exception) {


            Log.e(
                TAG,
                "Error obteniendo MAC",
                e
            )

        }



        // Último recurso
        val androidId =
            getAndroidId(context)


        Log.w(
            TAG,
            "No se pudo obtener MAC. Usando Android ID: $androidId"
        )


        return androidId

    }





    /**
     * Android ID del dispositivo
     * Solo como respaldo.
     */
    fun getAndroidId(context: Context): String {


        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
            ?: "unknown_device"

    }





    /**
     * Modelo del teléfono
     */
    fun getDeviceName(): String {


        return "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"


    }

}