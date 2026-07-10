package com.v2ray.ang.util

import android.content.Context
import com.v2ray.ang.util.MmkvManager

object AngConfigManager {
    fun importConfig(context: Context, serverLink: String): Boolean {
        return if (serverLink.isNotEmpty()) {
            MmkvManager.removeAllServer()
            val index = MmkvManager.importConfig(serverLink)
            index != null && index.isNotEmpty()
        } else {
            false
        }
    }
}
