package com.waterwise.app.util;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.waterwise.app.R;

import java.text.DateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/** Computes the "Water today / in 3 days / overdue" status of a plant. */
public final class WateringStatus {

    public static final int OK = 0;
    public static final int DUE = 1;
    public static final int OVERDUE = 2;

    private WateringStatus() {
    }

    /** Number of calendar days until the next watering (negative = overdue). */
    public static long daysUntil(long nextWateringAt) {
        LocalDate next = Instant.ofEpochMilli(nextWateringAt)
                .atZone(ZoneId.systemDefault()).toLocalDate();
        return ChronoUnit.DAYS.between(LocalDate.now(), next);
    }

    public static int state(long nextWateringAt) {
        long days = daysUntil(nextWateringAt);
        if (days < 0) return OVERDUE;
        if (days == 0) return DUE;
        return OK;
    }

    public static String label(Context context, long nextWateringAt) {
        int days = (int) daysUntil(nextWateringAt);
        if (days < 0) {
            return context.getResources().getQuantityString(R.plurals.status_overdue, -days, -days);
        }
        if (days == 0) {
            return context.getString(R.string.status_due_today);
        }
        return context.getResources().getQuantityString(R.plurals.status_in_days, days, days);
    }

    public static int color(Context context, long nextWateringAt) {
        switch (state(nextWateringAt)) {
            case OVERDUE:
                return ContextCompat.getColor(context, R.color.status_overdue);
            case DUE:
                return ContextCompat.getColor(context, R.color.status_due);
            default:
                return ContextCompat.getColor(context, R.color.status_ok);
        }
    }

    public static String formatDate(long millis) {
        return DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(millis));
    }
}
