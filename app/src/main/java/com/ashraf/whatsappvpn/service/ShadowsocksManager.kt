package com.v2ray.ang.service

import android.content.Context
import android.content.Intent

object V2rayServiceManager {
    fun startV2rayServer(context: Context) {
        try {
            val intent = Intent(context, Class.forName("com.v2ray.ang.service.V2rayService"))
            intent.putExtra("COMMAND", "START")
            context.startService(intent)
        } catch (e: Exception) {
            val intent = Intent(context, Class.forName("com.v2ray.ang.service.V2RayService"))
            intent.putExtra("COMMAND", "START")
            context.startService(intent)
        }
    }

    fun stopV2rayServer(context: Context) {
        try {
            val intent = Intent(context, Class.forName("com.v2ray.ang.service.V2rayService"))
            intent.putExtra("COMMAND", "STOP")
            context.startService(intent)
        } catch (e: Exception) {
            val intent = Intent(context, Class.forName("com.v2ray.ang.service.V2RayService"))
            intent.putExtra("COMMAND", "STOP")
            context.startService(intent)
        }
    }
}
