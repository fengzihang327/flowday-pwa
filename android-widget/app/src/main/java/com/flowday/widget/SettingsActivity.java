package com.flowday.widget;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

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

        TextView statusText = findViewById(R.id.status_text);
        statusText.setText(
                "✅ Flowday 小组件已就绪\n\n" +
                "▸ 在桌面添加小组件：长按桌面 → 小组件 → Flowday\n" +
                "▸ 打开 Flowday PWA 添加任务，数据自动同步\n" +
                "▸ 同步地址：localhost:18765\n\n" +
                "提示：首次使用需先打开一次此页面以启动同步服务。"
        );
    }
}
