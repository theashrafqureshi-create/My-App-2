package com.ashraf.whatsappvpn.service;

import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ShadowsocksLocalServer {
    private static final String TAG = "SSLocalServer";
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private final int localPort = 1080;

    public void startServer(String remoteServerIp, int remoteServerPort, String password, String method) {
        isRunning = true;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(localPort);
                Log.i(TAG, "Local Shadowsocks Server started on port " + localPort);

                while (isRunning) {
                    Socket localSocket = serverSocket.accept();
                    new Thread(() -> handleConnection(localSocket, remoteServerIp, remoteServerPort)).start();
                }
            } catch (IOException e) {
                Log.e(TAG, "Server error: " + e.getMessage());
            }
        }).start();
    }

    private void handleConnection(Socket localSocket, String remoteIp, int remotePort) {
        try (Socket remoteSocket = new Socket(remoteIp, remotePort);
             InputStream localIn = localSocket.getInputStream();
             OutputStream localOut = localSocket.getOutputStream();
             InputStream remoteIn = remoteSocket.getInputStream();
             OutputStream remoteOut = remoteSocket.getOutputStream()) {

            Thread t1 = new Thread(() -> {
                byte[] buffer = new byte[16384];
                int bytesRead;
                try {
                    while ((bytesRead = localIn.read(buffer)) != -1) {
                        remoteOut.write(buffer, 0, bytesRead);
                        remoteOut.flush();
                    }
                } catch (IOException ignored) {}
            });

            Thread t2 = new Thread(() -> {
                byte[] buffer = new byte[16384];
                int bytesRead;
                try {
                    while ((bytesRead = remoteIn.read(buffer)) != -1) {
                        localOut.write(buffer, 0, bytesRead);
                        localOut.flush();
                    }
                } catch (IOException ignored) {}
            });

            t1.start();
            t2.start();
            t1.join();
            t2.join();

        } catch (Exception e) {
            Log.e(TAG, "Data transfer error: " + e.getMessage());
        } finally {
            try {
                localSocket.close();
            } catch (IOException ignored) {}
        }
    }

    public void stopServer() {
        isRunning = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
                Log.d(TAG, "Local Server Stopped");
            } catch (IOException ignored) {}
        }
    }
}
