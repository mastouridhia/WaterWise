package com.waterwise.app;

import android.app.Application;

import com.waterwise.app.notifications.Notifications;
import com.waterwise.app.notifications.ReminderScheduler;

/** Runs once when the app process starts. */
public class WaterWiseApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Notifications.createChannel(this);
        ReminderScheduler.schedule(this);
    }
}
