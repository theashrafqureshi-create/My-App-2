package com.v2ray.ang

import android.app.Application
import com.tencent.mmkv.MMKV

class AppConfig : Application() {
    override fun onCreate() {
        super.onCreate()
        // ऐप शुरू होते ही डेटा स्टोरेज (MMKV) को चालू करने के लिए
        MMKV.initialize(this)
    }
}
