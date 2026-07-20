package com.flowday.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

public class FlowdayWidget extends AppWidgetProvider {

    private static final String PREFS_NAME = "flowday_data";
    private static final int MAX_TASKS = 5;

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

    private static int dotForKind(String kind) {
        if (kind == null) return R.drawable.dot_default;
        switch (kind) {
            case "focus":  return R.drawable.dot_focus;
            case "work":   return R.drawable.dot_work;
            case "health": return R.drawable.dot_health;
            case "life":   return R.drawable.dot_life;
            default:       return R.drawable.dot_default;
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

            views.setTextViewText(R.id.w_percent, pct + "%");
            views.setTextViewText(R.id.w_done, done + " / " + total);
            views.setTextViewText(R.id.w_msg, total == 0 ? "暂无任务"
                : pct == 100 ? "全部完成 🎉"
                : "剩余 " + (total - done) + " 项");

            int[] dotIds   = {R.id.w_dot1, R.id.w_dot2, R.id.w_dot3, R.id.w_dot4, R.id.w_dot5};
            int[] labelIds = {R.id.w_label1, R.id.w_label2, R.id.w_label3, R.id.w_label4, R.id.w_label5};
            int[] timeIds  = {R.id.w_time1, R.id.w_time2, R.id.w_time3, R.id.w_time4, R.id.w_time5};

            if (tasks != null && tasks.length() > 0) {
                int count = Math.min(tasks.length(), MAX_TASKS);
                for (int i = 0; i < count; i++) {
                    JSONObject t = tasks.getJSONObject(i);
                    boolean isDone = t.optBoolean("done", false);
                    String title = t.optString("title", "");
                    String start = t.optString("startTime", "");
                    String end = t.optString("endTime", "");
                    String kind = t.optString("kind", "");
                    String timeStr = "";
                    if (!start.isEmpty() && !end.isEmpty()) timeStr = start + "—" + end;
                    else if (!start.isEmpty()) timeStr = start;

                    views.setInt(dotIds[i], "setBackgroundResource",
                            isDone ? R.drawable.dot_done : dotForKind(kind));
                    // Strike-through for done tasks
                    String displayTitle = isDone ? title : title;
                    views.setTextViewText(labelIds[i], displayTitle);
                    views.setTextViewText(timeIds[i], timeStr);
                    views.setInt(labelIds[i], "setVisibility", android.view.View.VISIBLE);
                    views.setInt(timeIds[i], "setVisibility", android.view.View.VISIBLE);
                    views.setInt(dotIds[i], "setVisibility", android.view.View.VISIBLE);
                }
                for (int i = count; i < MAX_TASKS; i++) {
                    views.setInt(labelIds[i], "setVisibility", android.view.View.GONE);
                    views.setInt(timeIds[i], "setVisibility", android.view.View.GONE);
                    views.setInt(dotIds[i], "setVisibility", android.view.View.GONE);
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
                    views.setInt(labelIds[i], "setVisibility", android.view.View.GONE);
                    views.setInt(timeIds[i], "setVisibility", android.view.View.GONE);
                    views.setInt(dotIds[i], "setVisibility", android.view.View.GONE);
                }
            }

        } catch (Exception e) {
            views.setTextViewText(R.id.w_msg, "等待同步…");
            views.setInt(R.id.w_empty, "setVisibility", android.view.View.VISIBLE);
        }

        Intent openIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://fengzihang327.github.io/flowday-pwa/time-planner.html"));
        PendingIntent pending = PendingIntent.getActivity(context, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, pending);

        mgr.updateAppWidget(widgetId, views);
    }
}
