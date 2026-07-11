package com.ashraf.whatsappvpn

import android.app.Application
import com.tencent.mmkv.MMKV
import com.github.shadowsocks.Core

class AppConfig : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // ऐप शुरू होते ही डेटा स्टोर (MMKV) को चालू करने के लिए
        MMKV.initialize(this)
        
        // शैडोसॉक्स कोर इंजन को ऐप के साथ इनिशियलाइज करने के लिए
        Core.init(this, MainActivity::class.java)
    }
}
