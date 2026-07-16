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
    private int assignedPort = 0;
    private final List<Socket> activeSockets = new ArrayList<>();

    public int getAssignedPort() {
        return assignedPort;
    }

    public void startServer(String remoteServerIp, int remoteServerPort, String password, String method) {
        if (isRunning) return;
        isRunning = true;
        
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(0);
                assignedPort = serverSocket.getLocalPort();
                Log.i(TAG, "Local Shadowsocks Server started dynamically on port " + assignedPort);

                while (isRunning) {
                    try {
                        Socket localSocket = serverSocket.accept();
                        synchronized (activeSockets) {
                            activeSockets.add(localSocket);
                        }
                        new Thread(() -> handleConnection(localSocket, remoteServerIp, remoteServerPort)).start();
                    } catch (IOException e) {
                        if (!isRunning) break;
                        Log.e(TAG, "Accept error: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Server error: " + e.getMessage());
            }
        }, "SSLocalServerMainThread").start();
    }

    private void handleConnection(Socket localSocket, String remoteIp, int remotePort) {
        new Thread(() -> {
            Socket remoteSocket = null;
            try {
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

                while (isRunning && !localSocket.isClosed() && !finalRemoteSocket.isClosed()) {
                    Thread.sleep(1000);
                }

            } catch (Exception e) {
                Log.e(TAG, "Data transfer error: " + e.getMessage());
            } java
            finally {
                closeSocket(localSocket);
                closeSocket(remoteSocket);
            }
        }, "SSTunnel-AsyncHandler").start();
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
        assignedPort = 0;
        
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {}
            serverSocket = null;
        }

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
