package com.flowday.widget;

import android.app.IntentService;
import android.content.Intent;

/**
 * Simple service to trigger widget refresh.
 */
public class WidgetUpdateService extends IntentService {

    public WidgetUpdateService() {
        super("WidgetUpdateService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        FlowdayWidget.updateAllWidgets(this);
    }
}
