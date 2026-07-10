package com.v2ray.ang.service

import android.content.Context
import android.content.Intent

object V2rayServiceManager {
    fun startV2rayServer(context: Context) {
        val intent = Intent(context, V2rayService::class.java)
        intent.putExtra("COMMAND", "START")
        context.startService(intent)
    }

    fun stopV2rayServer(context: Context) {
        val intent = Intent(context, V2rayService::class.java)
        intent.putExtra("COMMAND", "STOP")
        context.startService(intent)
    }
}
