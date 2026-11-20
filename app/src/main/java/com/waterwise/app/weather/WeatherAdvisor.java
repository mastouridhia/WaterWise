package com.waterwise.app.weather;

import com.waterwise.app.data.Plant;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * The weather rules of WaterWise (only for OUTDOOR plants):
 *
 * 1. RAIN  – if it rained enough yesterday/today and the plant was not
 *            watered since yesterday, the rain counts as a watering:
 *            the next watering is postponed by a full interval.
 * 2. HEAT  – during a heat wave plants dry about twice as fast: the next
 *            watering is moved to half the interval after the last one.
 * 3. FROST – no schedule change, only an alert to protect the plants.
 *
 * Rules are "idempotent": applying them again with the same weather does
 * not change the plant again (important because the weather is refreshed
 * several times a day).
 */
public final class WeatherAdvisor {

    public enum Action {NONE, POSTPONE_RAIN, ADVANCE_HEAT}

    public static class Thresholds {
        public final double rainMm;
        public final double heatC;
        public final double frostC;

        public Thresholds(double rainMm, double heatC, double frostC) {
            this.rainMm = rainMm;
            this.heatC = heatC;
            this.frostC = frostC;
        }
    }

    /** Result for one plant. Dates are only meaningful when action != NONE. */
    public static class Advice {
        public static final Advice NONE = new Advice(Action.NONE, 0, 0);

        public final Action action;
        public final long newLastWateredAt;
        public final long newNextWateringAt;

        Advice(Action action, long newLastWateredAt, long newNextWateringAt) {
            this.action = action;
            this.newLastWateredAt = newLastWateredAt;
            this.newNextWateringAt = newNextWateringAt;
        }
    }

    private WeatherAdvisor() {
    }

    public static Advice advise(Plant plant, DailyWeather weather, Thresholds t,
                                long now, ZoneId zone) {
        if (!plant.outdoor) return Advice.NONE;

        LocalDate today = toDate(now, zone);
        long daysUntilNext = ChronoUnit.DAYS.between(today, toDate(plant.nextWateringAt, zone));

        // Rule 1: rain
        long startOfYesterday = today.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
        boolean rainedEnough = weather.recentRainMm() >= t.rainMm;
        boolean notWateredSinceYesterday = plant.lastWateredAt < startOfYesterday;
        if (rainedEnough && notWateredSinceYesterday && daysUntilNext <= 1) {
            long next = now + TimeUnit.DAYS.toMillis(plant.wateringIntervalDays);
            return new Advice(Action.POSTPONE_RAIN, now, next);
        }

        // Rule 2: heat
        if (weather.hottestNext2DaysC() >= t.heatC) {
            long halfInterval = TimeUnit.DAYS.toMillis(Math.max(1,
                    Math.round(plant.wateringIntervalDays / 2.0)));
            long heatNext = Math.max(plant.lastWateredAt + halfInterval, now);
            long daysUntilHeatNext = ChronoUnit.DAYS.between(today, toDate(heatNext, zone));
            // Only if it really moves the watering to an earlier day.
            if (daysUntilHeatNext < daysUntilNext) {
                return new Advice(Action.ADVANCE_HEAT, plant.lastWateredAt, heatNext);
            }
        }
        return Advice.NONE;
    }

    /** Rule 3: frost alert for the coming night / tomorrow. */
    public static boolean frostAlert(DailyWeather weather, Thresholds t) {
        return weather.coldestNext2DaysC() <= t.frostC;
    }

    public static boolean heatAlert(DailyWeather weather, Thresholds t) {
        return weather.hottestNext2DaysC() >= t.heatC;
    }

    public static boolean rainInfo(DailyWeather weather, Thresholds t) {
        return weather.recentRainMm() >= t.rainMm;
    }

    private static LocalDate toDate(long millis, ZoneId zone) {
        return Instant.ofEpochMilli(millis).atZone(zone).toLocalDate();
    }
}
