package com.waterwise.app.weather;

import com.waterwise.app.R;

/** WMO weather codes used by Open-Meteo → text and emoji. */
public final class WeatherCodes {

    private WeatherCodes() {
    }

    public static int description(Integer code) {
        if (code == null) return R.string.weather_unknown;
        if (code == 0) return R.string.weather_clear;
        if (code <= 3) return R.string.weather_cloudy;
        if (code == 45 || code == 48) return R.string.weather_fog;
        if (code >= 51 && code <= 57) return R.string.weather_drizzle;
        if (code >= 61 && code <= 67) return R.string.weather_rain;
        if (code >= 71 && code <= 77) return R.string.weather_snow;
        if (code >= 80 && code <= 82) return R.string.weather_showers;
        if (code == 85 || code == 86) return R.string.weather_snow;
        if (code >= 95) return R.string.weather_storm;
        return R.string.weather_unknown;
    }

    public static String emoji(Integer code) {
        if (code == null) return "🌡️";
        if (code == 0) return "☀️";
        if (code <= 2) return "🌤️";
        if (code == 3) return "☁️";
        if (code == 45 || code == 48) return "🌫️";
        if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) return "🌧️";
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) return "❄️";
        if (code >= 95) return "⛈️";
        return "🌡️";
    }
}
