package com.flowday.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

public class FlowdayWidget extends AppWidgetProvider {

    private static final String PREFS_NAME = "flowday_data";
    private static final int MAX_TASKS = 5;
    private static final String[] WD = {"日","一","二","三","四","五","六"};

    // Kind colors for task blocks
    private static int kindColor(String kind) {
        if (kind == null) return Color.parseColor("#2a2e2c");
        switch (kind) {
            case "focus":  return Color.parseColor("#3d2018"); // dark orange bg
            case "work":   return Color.parseColor("#1a2e20"); // dark green bg
            case "health": return Color.parseColor("#162238"); // dark blue bg
            case "life":   return Color.parseColor("#261e33"); // dark purple bg
            default:       return Color.parseColor("#2a2e2c"); // dark gray bg
        }
    }

    private static int kindTextColor(String kind) {
        if (kind == null) return Color.parseColor("#c5c8c7");
        switch (kind) {
            case "focus":  return Color.parseColor("#ff9578");
            case "work":   return Color.parseColor("#6de594");
            case "health": return Color.parseColor("#7ab8f5");
            case "life":   return Color.parseColor("#c0a0f5");
            default:       return Color.parseColor("#c5c8c7");
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int widgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId);
        }
    }

    public static void updateAllWidgets(Context context) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(context, FlowdayWidget.class));
        for (int id : ids) {
            updateWidget(context, mgr, id);
        }
    }

    private static String formatDate(String dateKey) {
        try {
            if (dateKey == null || dateKey.isEmpty()) return "";
            String[] parts = dateKey.split("-");
            if (parts.length != 3) return dateKey;
            int y = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            int d = Integer.parseInt(parts[2]);
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(y, m - 1, d);
            int dow = cal.get(java.util.Calendar.DAY_OF_WEEK) - 1; // Sun=0
            return "周" + WD[dow] + " · " + m + "/" + d;
        } catch (Exception e) {
            return dateKey != null ? dateKey : "";
        }
    }

    private static void updateWidget(Context context, AppWidgetManager mgr, int widgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString("tasks_json", "{}");

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        try {
            JSONObject data = new JSONObject(json);
            JSONArray tasks = data.optJSONArray("tasks");
            int done = data.optInt("done", 0);
            int total = data.optInt("total", 0);
            int pct = total > 0 ? Math.round((float) done / total * 100) : 0;
            String dateKey = data.optString("date", "");

            // Set date
            views.setTextViewText(R.id.w_date, formatDate(dateKey));

            // Progress
            views.setTextViewText(R.id.w_percent, pct + "%");
            views.setTextViewText(R.id.w_done, done + " / " + total);
            views.setTextViewText(R.id.w_msg, total == 0 ? "暂无任务"
                : pct == 100 ? "全部完成"
                : "剩余 " + (total - done) + " 项");

            int[] rowIds   = {R.id.w_row1, R.id.w_row2, R.id.w_row3, R.id.w_row4, R.id.w_row5};
            int[] labelIds = {R.id.w_label1, R.id.w_label2, R.id.w_label3, R.id.w_label4, R.id.w_label5};
            int[] timeIds  = {R.id.w_time1, R.id.w_time2, R.id.w_time3, R.id.w_time4, R.id.w_time5};
            int[] locIds   = {R.id.w_loc1, R.id.w_loc2, R.id.w_loc3, R.id.w_loc4, R.id.w_loc5};

            if (tasks != null && tasks.length() > 0) {
                int count = Math.min(tasks.length(), MAX_TASKS);
                for (int i = 0; i < count; i++) {
                    JSONObject t = tasks.getJSONObject(i);
                    boolean isDone = t.optBoolean("done", false);
                    String title = t.optString("title", "");
                    String start = t.optString("startTime", "");
                    String end = t.optString("endTime", "");
                    String loc = t.optString("location", "");
                    String kind = t.optString("kind", "");

                    String timeStr = "";
                    if (!start.isEmpty() && !end.isEmpty()) timeStr = start + "—" + end;
                    else if (!start.isEmpty()) timeStr = start;

                    // Set colored background block for the row
                    int bgColor = isDone ? Color.parseColor("#1a1d1c") : kindColor(kind);
                    views.setInt(rowIds[i], "setBackgroundColor", bgColor);

                    // Title with kind-based text color
                    int textColor = isDone ? Color.parseColor("#5a5f5d") : kindTextColor(kind);
                    String displayTitle = isDone ? "✓ " + title : title;
                    views.setTextViewText(labelIds[i], displayTitle);
                    views.setTextColor(labelIds[i], textColor);

                    // Time
                    views.setTextViewText(timeIds[i], timeStr);
                    views.setTextColor(timeIds[i], isDone ? Color.parseColor("#4a4f4d") : Color.parseColor("#a0a5a3"));

                    // Location
                    String locText = !loc.isEmpty() ? "📍 " + loc : "";
                    views.setTextViewText(locIds[i], locText);

                    views.setInt(rowIds[i], "setVisibility", android.view.View.VISIBLE);
                }
                for (int i = count; i < MAX_TASKS; i++) {
                    views.setInt(rowIds[i], "setVisibility", android.view.View.GONE);
                }
                views.setInt(R.id.w_more, "setVisibility",
                        tasks.length() > MAX_TASKS ? android.view.View.VISIBLE : android.view.View.GONE);
                if (tasks.length() > MAX_TASKS) {
                    views.setTextViewText(R.id.w_more, "+" + (tasks.length() - MAX_TASKS) + " 项");
                }
                views.setInt(R.id.w_empty, "setVisibility", android.view.View.GONE);
            } else {
                views.setInt(R.id.w_empty, "setVisibility", android.view.View.VISIBLE);
                views.setInt(R.id.w_more, "setVisibility", android.view.View.GONE);
                for (int i = 0; i < MAX_TASKS; i++) {
                    views.setInt(rowIds[i], "setVisibility", android.view.View.GONE);
                }
            }

        } catch (Exception e) {
            views.setTextViewText(R.id.w_msg, "等待同步…");
            views.setInt(R.id.w_empty, "setVisibility", android.view.View.VISIBLE);
        }

        // Try to launch PWA in standalone mode
        String pwaUrl = "https://fengzihang327.github.io/flowday-pwa/";
        Intent openIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(pwaUrl));
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        openIntent.addCategory(Intent.CATEGORY_BROWSABLE);

        // Search all installed apps for one that can handle our PWA URL as LAUNCHER
        android.content.pm.PackageManager pm = context.getPackageManager();
        Intent launcherIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(pwaUrl));
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        launcherIntent.addCategory(Intent.CATEGORY_BROWSABLE);
        java.util.List<android.content.pm.ResolveInfo> handlers = pm.queryIntentActivities(launcherIntent,
                android.content.pm.PackageManager.MATCH_DEFAULT_ONLY);

        // Prefer Samsung Internet or Chrome
        String targetPkg = null;
        for (android.content.pm.ResolveInfo ri : handlers) {
            String p = ri.activityInfo.packageName;
            if (p.equals("com.sec.android.app.sbrowser")) { targetPkg = p; break; }
            if (p.equals("com.android.chrome") && targetPkg == null) { targetPkg = p; }
        }
        // If neither found, use the first handler
        if (targetPkg == null && !handlers.isEmpty()) {
            targetPkg = handlers.get(0).activityInfo.packageName;
        }

        if (targetPkg != null) {
            openIntent.setPackage(targetPkg);
        }

        PendingIntent pending = PendingIntent.getActivity(context, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, pending);

        mgr.updateAppWidget(widgetId, views);
    }
}
