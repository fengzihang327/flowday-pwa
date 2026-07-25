package com.flowday.widget;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Minimal settings/info activity.
 * Opens when user taps the widget app icon.
 * Also starts the local server.
 */
public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Start the local server
        LocalServerService.start(this);

        // Request exact alarm permission for reliable notifications
        AlarmManager am = getSystemService(AlarmManager.class);
        if (am != null && !am.canScheduleExactAlarms()) {
            try {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
                Toast.makeText(this, "请允许精确闹钟以接收任务提醒", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "请在设置中允许闹钟权限", Toast.LENGTH_LONG).show();
            }
        }

        TextView statusText = findViewById(R.id.status_text);
        statusText.setText(
                "✅ Flowday 小组件已就绪\n\n" +
                "===== 安装 PWA =====\n" +
                "1. Samsung Internet 打开:\n" +
                "   fengzihang327.github.io/flowday-pwa/\n" +
                "2. 地址栏 → 添加到主屏幕\n\n" +
                "===== 通知提醒 =====\n" +
                "1. 设置 → 应用程序 → Flowday\n" +
                "2. 电池 → 不受限制\n" +
                "3. 通知 → 允许\n\n" +
                "===== 小组件 =====\n" +
                "长按桌面 → 小组件 → Flowday\n" +
                "点击小组件打开 PWA\n\n" +
                "同步服务: localhost:18765"
        );
    }
}
