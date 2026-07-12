package com.ashraf.whatsappvpn.service;

import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ShadowsocksLocalServer {
    private static final String TAG = "SSLocalServer";
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private final int localPort = 1080;
    private final List<Socket> activeSockets = new ArrayList<>();

    public void startServer(String remoteServerIp, int remoteServerPort, String password, String method) {
        if (isRunning) return; // अगर सर्वर पहले से चल रहा है तो दोबारा स्टार्ट न करें
        isRunning = true;
        
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(localPort);
                Log.i(TAG, "Local Shadowsocks Server started on port " + localPort);

                while (isRunning) {
                    try {
                        Socket localSocket = serverSocket.accept();
                        synchronized (activeSockets) {
                            activeSockets.add(localSocket);
                        }
                        // 🎯 Android 14 सेफ: पूरी तरह अलग बैकग्राउंड थ्रेड में नेटवर्क हैंडलिंग
                        new Thread(() -> handleConnection(localSocket, remoteServerIp, remoteServerPort)).start();
                    } catch (IOException e) {
                        if (!isRunning) break; // अगर सर्वर स्टॉप हुआ है तो एरर को इग्नोर करें
                        Log.e(TAG, "Accept error: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Server error: " + e.getMessage());
            }
        }, "SSLocalServerMainThread").start();
    }

    private void handleConnection(Socket localSocket, String remoteIp, int remotePort) {
        Socket remoteSocket = null;
        try {
            // 🎯 थ्रेड के अंदर ही सॉकेट बनाना ताकि NetworkOnMainThread एरर न आए
            remoteSocket = new Socket(remoteIp, remotePort);
            synchronized (activeSockets) {
                activeSockets.add(remoteSocket);
            }

            final Socket finalRemoteSocket = remoteSocket;
            InputStream localIn = localSocket.getInputStream();
            OutputStream localOut = localSocket.getOutputStream();
            InputStream remoteIn = remoteSocket.getInputStream();
            OutputStream remoteOut = remoteSocket.getOutputStream();

            Thread t1 = new Thread(() -> {
                byte[] buffer = new byte[16384];
                int bytesRead;
                try {
                    while (isRunning && (bytesRead = localIn.read(buffer)) != -1) {
                        remoteOut.write(buffer, 0, bytesRead);
                        remoteOut.flush();
                    }
                } catch (IOException ignored) {}
            }, "SSTunnel-LocalToRemote");

            Thread t2 = new Thread(() -> {
                byte[] buffer = new byte[16384];
                int bytesRead;
                try {
                    while (isRunning && (bytesRead = remoteIn.read(buffer)) != -1) {
                        localOut.write(buffer, 0, bytesRead);
                        localOut.flush();
                    }
                } catch (IOException ignored) {}
            }, "SSTunnel-RemoteToLocal");

            t1.start();
            t2.start();
            t1.join();
            t2.join();

        } catch (Exception e) {
            Log.e(TAG, "Data transfer error: " + e.getMessage());
        } finally {
            // सेफ क्लोजिंग लॉजिक
            closeSocket(localSocket);
            closeSocket(remoteSocket);
        }
    }

    private void closeSocket(Socket socket) {
        if (socket != null) {
            try {
                synchronized (activeSockets) {
                    activeSockets.remove(socket);
                }
                if (!socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException ignored) {}
        }
    }

    public void stopServer() {
        isRunning = false;
        
        // 1. मेन सर्वर सॉकेट बंद करना
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {}
            serverSocket = null;
        }

        // 2. सभी एक्टिव क्लाइंट और रिमोट सॉकेट्स को तुरंत किल करना ताकि ऐप हैंग न हो
        synchronized (activeSockets) {
            for (Socket socket : activeSockets) {
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException ignored) {}
            }
            activeSockets.clear();
        }
        Log.d(TAG, "Local Server and all connections stopped safely");
    }
}
