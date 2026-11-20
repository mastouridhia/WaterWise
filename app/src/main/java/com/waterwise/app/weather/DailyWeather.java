package com.waterwise.app.weather;

/**
 * The few numbers the watering rules need, extracted from the forecast.
 * Pure Java so the rules can be unit-tested without Android.
 */
public class DailyWeather {

    public final double rainYesterdayMm;
    public final double rainTodayMm;
    public final double maxTodayC;
    public final double maxTomorrowC;
    public final double minTodayC;
    public final double minTomorrowC;

    public DailyWeather(double rainYesterdayMm, double rainTodayMm,
                        double maxTodayC, double maxTomorrowC,
                        double minTodayC, double minTomorrowC) {
        this.rainYesterdayMm = rainYesterdayMm;
        this.rainTodayMm = rainTodayMm;
        this.maxTodayC = maxTodayC;
        this.maxTomorrowC = maxTomorrowC;
        this.minTodayC = minTodayC;
        this.minTomorrowC = minTomorrowC;
    }

    /** Rain fallen yesterday + expected today. */
    public double recentRainMm() {
        return rainYesterdayMm + rainTodayMm;
    }

    public double hottestNext2DaysC() {
        return Math.max(maxTodayC, maxTomorrowC);
    }

    public double coldestNext2DaysC() {
        return Math.min(minTodayC, minTomorrowC);
    }
}
