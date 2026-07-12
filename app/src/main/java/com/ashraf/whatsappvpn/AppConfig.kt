package com.ashraf.whatsappvpn

import android.app.Application

class AppConfig : Application() {
    override fun onCreate() {
        super.onCreate()
        // पुरानी लाइब्रेरीज (MMKV और Core) वाला कोड यहाँ से हटा दिया गया है
        // क्योंकि उनकी अब कोई ज़रूरत नहीं है।
    }
}
