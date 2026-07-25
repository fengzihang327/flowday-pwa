package com.flowday.widget;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

/**
 * Fired by AlarmManager when a task notification is due.
 */
public class NotificationReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "flowday_tasks";
    private static final String CHANNEL_NAME = "任务提醒";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        String body = intent.getStringExtra("body");
        int id = intent.getIntExtra("notify_id", 0);

        if (title == null) title = "⏰ 任务时间";

        createChannel(context);

        android.app.Notification notification = new android.app.Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(body != null ? body : "")
                .setPriority(android.app.Notification.PRIORITY_HIGH)
                .setDefaults(android.app.Notification.DEFAULT_SOUND | android.app.Notification.DEFAULT_VIBRATE)
                .setAutoCancel(true)
                .setContentIntent(buildOpenIntent(context))
                .build();

        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.notify(id, notification);
        }
    }

    private void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Flowday 任务到时间提醒");
            channel.enableVibration(true);
            channel.setSound(null, null); // Use default sound
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private PendingIntent buildOpenIntent(Context context) {
        Intent open = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://fengzihang327.github.io/flowday-pwa/time-planner.html"));
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * Schedule all task alarms. Call this when task data changes.
     */
    public static void scheduleAlarms(Context context, String tasksJson) {
        AlarmManager am = context.getSystemService(AlarmManager.class);
        if (am == null) return;

        // Cancel all existing flowday alarms
        cancelAllAlarms(context);

        try {
            org.json.JSONObject data = new org.json.JSONObject(tasksJson);
            org.json.JSONArray tasks = data.optJSONArray("tasks");

            if (tasks == null || tasks.length() == 0) return;

            java.util.Calendar now = java.util.Calendar.getInstance();

            for (int i = 0; i < tasks.length(); i++) {
                org.json.JSONObject t = tasks.getJSONObject(i);
                if (t.optBoolean("done", false)) continue;

                String startTime = t.optString("startTime", "");
                if (startTime.isEmpty()) continue;

                String[] parts = startTime.split(":");
                int hour = Integer.parseInt(parts[0]);
                int minute = Integer.parseInt(parts[1]);

                java.util.Calendar target = java.util.Calendar.getInstance();
                target.set(java.util.Calendar.HOUR_OF_DAY, hour);
                target.set(java.util.Calendar.MINUTE, minute);
                target.set(java.util.Calendar.SECOND, 0);
                target.set(java.util.Calendar.MILLISECOND, 0);

                // Notify 5 minutes before
                target.add(java.util.Calendar.MINUTE, -5);

                // If time already passed today, skip
                if (target.before(now)) continue;

                String title = "⏰ " + t.optString("title", "任务");
                String loc = t.optString("location", "");
                String body = (!loc.isEmpty() ? "📍 " + loc + " · " : "") + "🕐 " + startTime;

                Intent intent = new Intent(context, NotificationReceiver.class);
                intent.putExtra("title", title);
                intent.putExtra("body", body);
                intent.putExtra("notify_id", 1000 + i);

                PendingIntent pi = PendingIntent.getBroadcast(context, 1000 + i, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.getTimeInMillis(), pi);
                } else {
                    am.set(AlarmManager.RTC_WAKEUP, target.getTimeInMillis(), pi);
                }
            }
        } catch (Exception e) {
            // Silently handle JSON parse errors
        }
    }

    private static void cancelAllAlarms(Context context) {
        AlarmManager am = context.getSystemService(AlarmManager.class);
        if (am == null) return;
        for (int i = 0; i < 100; i++) {
            Intent intent = new Intent(context, NotificationReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(context, 1000 + i, intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pi != null) {
                am.cancel(pi);
                pi.cancel();
            }
        }
    }
}
