package com.waterwise.app.notifications;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.waterwise.app.R;
import com.waterwise.app.data.AppDatabase;
import com.waterwise.app.data.Plant;
import com.waterwise.app.util.AppSettings;
import com.waterwise.app.weather.WeatherAdvisor;
import com.waterwise.app.weather.WeatherReport;
import com.waterwise.app.weather.WeatherRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs once a day in the background (WorkManager), even when the app is
 * closed or after a reboot:
 * 1. downloads the forecast and adjusts outdoor plants (rain / heat);
 * 2. notifies the user about plants to water today and frost.
 */
public class DailyCheckWorker extends Worker {

    public DailyCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        AppSettings settings = new AppSettings(context);

        // 1. Weather (optional: without internet we still remind the user)
        WeatherReport report = null;
        WeatherRepository weather = new WeatherRepository(context);
        if (settings.hasCity()) {
            try {
                report = weather.fetchAndApplyRules();
            } catch (IOException ignored) {
                // Offline: skip the weather part.
            }
        }

        // 2. Plants due today (after the weather adjustments)
        long endOfToday = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli();
        List<Plant> due = AppDatabase.get(context).plantDao().getDueBeforeSync(endOfToday);

        boolean frost = report != null
                && WeatherAdvisor.frostAlert(report.daily, weather.thresholds());
        int adjusted = report != null ? report.adjustedPlants : 0;

        if (due.isEmpty() && !frost && adjusted == 0) {
            return Result.success(); // nothing worth a notification
        }

        String title = due.isEmpty()
                ? context.getString(R.string.notif_title_none)
                : context.getResources().getQuantityString(R.plurals.notif_title_due,
                due.size(), due.size());

        List<String> lines = new ArrayList<>();
        if (!due.isEmpty()) lines.add(names(due));
        if (frost) lines.add(context.getString(R.string.notif_frost));
        if (adjusted > 0) {
            lines.add(context.getResources().getQuantityString(R.plurals.notif_adjusted,
                    adjusted, adjusted));
        }
        Notifications.showDaily(context, title, String.join("\n", lines));
        return Result.success();
    }

    private static String names(List<Plant> plants) {
        List<String> names = new ArrayList<>();
        for (Plant p : plants) names.add(p.nickname);
        return String.join(", ", names);
    }
}
