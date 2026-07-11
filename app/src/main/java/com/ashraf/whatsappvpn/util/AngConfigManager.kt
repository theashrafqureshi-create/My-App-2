package com.v2ray.ang.util

import android.content.Context

object AngConfigManager {
    fun importConfig(context: Context, serverLink: String): Boolean {
        return if (serverLink.isNotEmpty()) {
            try {
                val clazz = Class.forName("com.v2ray.ang.util.MmkvManager")
                val method = clazz.getDeclaredMethod("importConfig", String::class.java)
                val index = method.invoke(null, serverLink)
                index != null
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }
}
