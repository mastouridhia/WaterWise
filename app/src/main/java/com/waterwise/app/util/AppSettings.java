package com.waterwise.app.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * User settings saved in SharedPreferences (a small key-value file):
 * the city for the weather and the thresholds of the weather rules.
 */
public class AppSettings {

    public static final float DEFAULT_RAIN_MM = 5f;
    public static final float DEFAULT_HEAT_C = 32f;
    public static final float DEFAULT_FROST_C = 2f;

    private static final String FILE = "waterwise_settings";
    private static final String KEY_CITY = "city";
    private static final String KEY_LAT = "lat";
    private static final String KEY_LON = "lon";
    private static final String KEY_RAIN_MM = "rain_mm";
    private static final String KEY_HEAT_C = "heat_c";
    private static final String KEY_FROST_C = "frost_c";
    private static final String KEY_REMINDER_ON = "reminder_on";
    private static final String KEY_REMINDER_HOUR = "reminder_hour";
    private static final String KEY_REMINDER_MINUTE = "reminder_minute";
    private static final String KEY_LAST_FORECAST = "last_forecast_json";
    private static final String KEY_LAST_FORECAST_AT = "last_forecast_at";

    private final SharedPreferences prefs;

    public AppSettings(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    // ---- City ----

    public boolean hasCity() {
        return prefs.contains(KEY_CITY);
    }

    public String getCityName() {
        return prefs.getString(KEY_CITY, null);
    }

    public double getLatitude() {
        return Double.longBitsToDouble(prefs.getLong(KEY_LAT, 0));
    }

    public double getLongitude() {
        return Double.longBitsToDouble(prefs.getLong(KEY_LON, 0));
    }

    /** Saves a new city and forgets the forecast of the previous one. */
    public void setCity(String name, double latitude, double longitude) {
        prefs.edit()
                .putString(KEY_CITY, name)
                // SharedPreferences has no putDouble: store the raw bits in a long.
                .putLong(KEY_LAT, Double.doubleToRawLongBits(latitude))
                .putLong(KEY_LON, Double.doubleToRawLongBits(longitude))
                .remove(KEY_LAST_FORECAST)
                .remove(KEY_LAST_FORECAST_AT)
                .apply();
    }

    // ---- Weather rule thresholds ----

    public float getRainThresholdMm() {
        return prefs.getFloat(KEY_RAIN_MM, DEFAULT_RAIN_MM);
    }

    public void setRainThresholdMm(float value) {
        prefs.edit().putFloat(KEY_RAIN_MM, value).apply();
    }

    public float getHeatThresholdC() {
        return prefs.getFloat(KEY_HEAT_C, DEFAULT_HEAT_C);
    }

    public void setHeatThresholdC(float value) {
        prefs.edit().putFloat(KEY_HEAT_C, value).apply();
    }

    public float getFrostThresholdC() {
        return prefs.getFloat(KEY_FROST_C, DEFAULT_FROST_C);
    }

    public void setFrostThresholdC(float value) {
        prefs.edit().putFloat(KEY_FROST_C, value).apply();
    }

    // ---- Daily reminder ----

    public boolean isReminderEnabled() {
        return prefs.getBoolean(KEY_REMINDER_ON, true);
    }

    public void setReminderEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_REMINDER_ON, enabled).apply();
    }

    public int getReminderHour() {
        return prefs.getInt(KEY_REMINDER_HOUR, 8);
    }

    public int getReminderMinute() {
        return prefs.getInt(KEY_REMINDER_MINUTE, 0);
    }

    public void setReminderTime(int hour, int minute) {
        prefs.edit().putInt(KEY_REMINDER_HOUR, hour).putInt(KEY_REMINDER_MINUTE, minute).apply();
    }

    // ---- Last forecast (shown when offline) ----

    public String getLastForecastJson() {
        return prefs.getString(KEY_LAST_FORECAST, null);
    }

    public long getLastForecastAt() {
        return prefs.getLong(KEY_LAST_FORECAST_AT, 0);
    }

    public void setLastForecast(String json, long time) {
        prefs.edit().putString(KEY_LAST_FORECAST, json).putLong(KEY_LAST_FORECAST_AT, time).apply();
    }
}
