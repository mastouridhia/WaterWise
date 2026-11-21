package com.waterwise.app.notifications;

import android.content.Context;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.waterwise.app.util.AppSettings;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

/** Schedules DailyCheckWorker every day at the time chosen in Settings. */
public final class ReminderScheduler {

    private static final String WORK_NAME = "daily_check";

    private ReminderScheduler() {
    }

    /** Call at app start and whenever the reminder settings change. */
    public static void schedule(Context context) {
        AppSettings settings = new AppSettings(context);
        WorkManager workManager = WorkManager.getInstance(context);
        if (!settings.isReminderEnabled()) {
            workManager.cancelUniqueWork(WORK_NAME);
            return;
        }

        long delay = millisUntil(settings.getReminderHour(), settings.getReminderMinute());
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                DailyCheckWorker.class, 1, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build();
        // Unique work: never two daily checks at the same time. Re-enqueue so a
        // new reminder time takes effect immediately.
        workManager.enqueueUniquePeriodicWork(WORK_NAME,
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request);
    }

    /** Runs the check right now (the "test notification" button). */
    public static void runNow(Context context) {
        WorkManager.getInstance(context)
                .enqueue(new OneTimeWorkRequest.Builder(DailyCheckWorker.class).build());
    }

    /** Milliseconds from now until the next hour:minute (today or tomorrow). */
    static long millisUntil(int hour, int minute) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = now.with(LocalTime.of(hour, minute));
        if (!next.isAfter(now)) next = next.plusDays(1);
        return Duration.between(now, next).toMillis();
    }
}
