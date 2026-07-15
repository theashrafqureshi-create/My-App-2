package com.ashraf.whatsappvpn;

import android.app.Application;
import com.tencent.mmkv.MMKV;

public class AppConfig extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // 🎯 [CRITICAL FIX] वापस आई mmkv.aar लाइब्रेरी को यहाँ इनिशियलाइज कर दिया है ताकि ऐप क्रैश न हो
        MMKV.initialize(this);
    }
}
