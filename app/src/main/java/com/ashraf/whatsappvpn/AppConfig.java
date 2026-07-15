Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        // एरर का पूरा संदेश और उसका स्टैक ट्रेस (कहाँ गड़बड़ हुई) एक साथ निकालने के लिए
        StringBuilder errorReport = new StringBuilder();
        errorReport.append("Error: ").append(throwable.getMessage()).append("\n\n");
        for (StackTraceElement element : throwable.getStackTrace()) {
            errorReport.append(element.toString()).append("\n");
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.Looper.prepare();
                
                // 🎯 यहाँ हम एक कस्टम TextView बना रहे हैं जो डिब्बे को पूरा फैला देगा
                android.widget.TextView textView = new android.widget.TextView(getApplicationContext());
                textView.setText(errorReport.toString()); // पूरा एरर कोड इसमें डाल दिया
                textView.setTextColor(android.graphics.Color.WHITE);
                textView.setBackgroundColor(android.graphics.Color.parseColor("#CC000000")); // टोस्ट जैसा काला बैकग्राउंड
                textView.setPadding(30, 30, 30, 30);
                textView.setTextSize(12); // छोटा साइज ताकि ज्यादा से ज्यादा लाइनें आ सकें
                
                // फ्रेश टोस्ट जनरेट करना
                android.widget.Toast toast = new android.widget.Toast(getApplicationContext());
                toast.setView(textView); // डिफ़ॉल्ट छोटे डिब्बे की जगह हमारा यह नया बड़ा डिब्बा सेट कर दिया
                toast.setDuration(android.widget.Toast.LENGTH_LONG);
                toast.show();
                
                android.os.Looper.loop();
            }
        }).start();

        // 4 सेकंड रुकेंगे ताकि तुम आराम से पूरा एरर पढ़ सको या स्क्रीनशॉट ले सको
        try { Thread.sleep(4000); } catch (InterruptedException e) {}
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }
});
