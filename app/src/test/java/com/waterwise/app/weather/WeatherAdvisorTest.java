package com.waterwise.app.weather;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.waterwise.app.data.Plant;

import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

public class WeatherAdvisorTest {

    private static final ZoneId ZONE = ZoneOffset.UTC;
    /** "Now" = 20 Nov 2026 at 10:00. */
    private static final long NOW = at(2026, 11, 20, 10);
    private static final WeatherAdvisor.Thresholds T =
            new WeatherAdvisor.Thresholds(5, 32, 2);

    private static final DailyWeather MILD = new DailyWeather(0, 0, 22, 23, 12, 11);
    private static final DailyWeather RAINY = new DailyWeather(6, 3, 18, 19, 10, 9);
    private static final DailyWeather HOT = new DailyWeather(0, 0, 35, 36, 22, 23);
    private static final DailyWeather FROSTY = new DailyWeather(0, 0, 6, 5, 1, -2);

    private static long at(int y, int m, int d, int hour) {
        return LocalDateTime.of(y, m, d, hour, 0).toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    private static long days(int n) {
        return TimeUnit.DAYS.toMillis(n);
    }

    /** Outdoor plant watered `daysAgo` days ago, every `interval` days. */
    private static Plant plant(boolean outdoor, int interval, int daysAgo) {
        Plant p = new Plant();
        p.outdoor = outdoor;
        p.wateringIntervalDays = interval;
        p.lastWateredAt = NOW - days(daysAgo);
        p.nextWateringAt = p.lastWateredAt + days(interval);
        return p;
    }

    @Test
    public void indoorPlantIsNeverChanged() {
        Plant p = plant(false, 7, 7);
        assertEquals(WeatherAdvisor.Action.NONE,
                WeatherAdvisor.advise(p, RAINY, T, NOW, ZONE).action);
    }

    @Test
    public void mildWeatherChangesNothing() {
        Plant p = plant(true, 7, 6);
        assertEquals(WeatherAdvisor.Action.NONE,
                WeatherAdvisor.advise(p, MILD, T, NOW, ZONE).action);
    }

    @Test
    public void rainPostponesAPlantDueToday() {
        Plant p = plant(true, 7, 7); // due today
        WeatherAdvisor.Advice a = WeatherAdvisor.advise(p, RAINY, T, NOW, ZONE);
        assertEquals(WeatherAdvisor.Action.POSTPONE_RAIN, a.action);
        assertEquals(NOW + days(7), a.newNextWateringAt);
        assertEquals(NOW, a.newLastWateredAt);
    }

    @Test
    public void rainDoesNotPostponeAPlantDueInAWeek() {
        Plant p = plant(true, 10, 2); // due in 8 days
        assertEquals(WeatherAdvisor.Action.NONE,
                WeatherAdvisor.advise(p, RAINY, T, NOW, ZONE).action);
    }

    @Test
    public void rainIsAppliedOnlyOnce() {
        Plant p = plant(true, 2, 2); // short interval, due today
        WeatherAdvisor.Advice a = WeatherAdvisor.advise(p, RAINY, T, NOW, ZONE);
        p.lastWateredAt = a.newLastWateredAt;
        p.nextWateringAt = a.newNextWateringAt;
        // Next day, the same rain (now "yesterday") must not postpone again.
        long tomorrow = NOW + days(1);
        assertEquals(WeatherAdvisor.Action.NONE,
                WeatherAdvisor.advise(p, RAINY, T, tomorrow, ZONE).action);
    }

    @Test
    public void lightRainBelowThresholdChangesNothing() {
        Plant p = plant(true, 7, 7);
        DailyWeather drizzle = new DailyWeather(1, 2, 18, 19, 10, 9);
        assertEquals(WeatherAdvisor.Action.NONE,
                WeatherAdvisor.advise(p, drizzle, T, NOW, ZONE).action);
    }

    @Test
    public void heatMovesWateringToHalfTheInterval() {
        Plant p = plant(true, 10, 2); // due in 8 days
        WeatherAdvisor.Advice a = WeatherAdvisor.advise(p, HOT, T, NOW, ZONE);
        assertEquals(WeatherAdvisor.Action.ADVANCE_HEAT, a.action);
        assertEquals(p.lastWateredAt + days(5), a.newNextWateringAt); // in 3 days
    }

    @Test
    public void heatIsAppliedOnlyOnce() {
        Plant p = plant(true, 10, 2);
        WeatherAdvisor.Advice a = WeatherAdvisor.advise(p, HOT, T, NOW, ZONE);
        p.nextWateringAt = a.newNextWateringAt;
        assertEquals(WeatherAdvisor.Action.NONE,
                WeatherAdvisor.advise(p, HOT, T, NOW + TimeUnit.HOURS.toMillis(3), ZONE).action);
    }

    @Test
    public void heatNeverSchedulesInThePast() {
        Plant p = plant(true, 10, 7); // half interval (5 days) already passed
        WeatherAdvisor.Advice a = WeatherAdvisor.advise(p, HOT, T, NOW, ZONE);
        assertEquals(WeatherAdvisor.Action.ADVANCE_HEAT, a.action);
        assertEquals(NOW, a.newNextWateringAt); // "water today"
    }

    @Test
    public void alerts() {
        assertTrue(WeatherAdvisor.frostAlert(FROSTY, T));
        assertFalse(WeatherAdvisor.frostAlert(MILD, T));
        assertTrue(WeatherAdvisor.heatAlert(HOT, T));
        assertTrue(WeatherAdvisor.rainInfo(RAINY, T));
        assertFalse(WeatherAdvisor.rainInfo(MILD, T));
    }
}
