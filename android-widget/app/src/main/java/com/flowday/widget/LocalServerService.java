package com.flowday.widget;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Minimal foreground service running a local HTTP server.
 * The Flowday PWA POSTs task data to http://localhost:18765/update
 */
public class LocalServerService extends Service {

    private static final String TAG = "FlowdayServer";
    private static final int PORT = 18765;
    private static final String PREFS_NAME = "flowday_data";
    private static final String CHANNEL_ID = "flowday_server";

    private ServerSocket serverSocket;
    private Thread serverThread;
    private volatile boolean running = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, buildNotification());
        startServer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopServer();
        super.onDestroy();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Flowday 同步服务",
                    NotificationManager.IMPORTANCE_MIN);
            channel.setDescription("保持小组件数据同步");
            channel.setShowBadge(false);
            NotificationManager mgr = getSystemService(NotificationManager.class);
            if (mgr != null) mgr.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Flowday 同步")
                .setContentText("小组件数据同步中")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setOngoing(true)
                .build();
    }

    private void startServer() {
        running = true;
        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT, 1);
                Log.i(TAG, "Server started on port " + PORT);

                while (running) {
                    try {
                        Socket client = serverSocket.accept();
                        handleRequest(client);
                        client.close();
                    } catch (Exception e) {
                        if (running) Log.e(TAG, "Connection error", e);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Server error", e);
            }
        }, "FlowdayServer");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    private void stopServer() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (Exception e) {
            // ignore
        }
        if (serverThread != null) serverThread.interrupt();
    }

    private void handleRequest(Socket client) {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
            String line = reader.readLine();
            if (line == null) return;

            // Parse: POST /update HTTP/1.1
            String[] parts = line.split(" ");
            boolean isPost = parts.length > 0 && "POST".equalsIgnoreCase(parts[0]);
            boolean isUpdate = parts.length > 1 && "/update".equals(parts[1]);

            // Read headers to find Content-Length
            int contentLength = 0;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                if (line.toLowerCase().startsWith("content-length:")) {
                    contentLength = Integer.parseInt(line.substring(15).trim());
                }
            }

            // Read body
            String body = "";
            if (contentLength > 0) {
                char[] buf = new char[contentLength];
                int read = reader.read(buf, 0, contentLength);
                if (read > 0) body = new String(buf, 0, read);
            }

            // Respond
            String response;
            if (isPost && isUpdate && !body.isEmpty()) {
                // Save data
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                prefs.edit().putString("tasks_json", body).apply();

                // Update widget
                FlowdayWidget.updateAllWidgets(this);

                response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n" +
                        "Access-Control-Allow-Origin: *\r\n\r\nOK";
                Log.i(TAG, "Widget updated with " + body.length() + " bytes");
            } else if ("OPTIONS".equalsIgnoreCase(parts[0])) {
                response = "HTTP/1.1 200 OK\r\n" +
                        "Access-Control-Allow-Origin: *\r\n" +
                        "Access-Control-Allow-Methods: POST, OPTIONS\r\n" +
                        "Access-Control-Allow-Headers: Content-Type\r\n\r\n";
            } else {
                response = "HTTP/1.1 404 Not Found\r\nContent-Type: text/plain\r\n" +
                        "Access-Control-Allow-Origin: *\r\n\r\nNot Found";
            }

            client.getOutputStream().write(response.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            Log.e(TAG, "Request handling error", e);
        }
    }

    /**
     * Start this service. Called from other components.
     */
    public static void start(Context context) {
        Intent intent = new Intent(context, LocalServerService.class);
        context.startForegroundService(intent);
    }
}
