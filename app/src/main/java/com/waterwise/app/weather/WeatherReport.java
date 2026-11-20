package com.waterwise.app.weather;

/** Everything the Today screen shows about the weather. */
public class WeatherReport {

    public final String city;
    public final Double currentTempC;
    public final Integer weatherCode;
    public final DailyWeather daily;
    /** True when the network failed and the last saved forecast is shown. */
    public final boolean fromCache;
    public final long fetchedAt;
    /** Number of outdoor plants whose schedule was changed by this refresh. */
    public final int adjustedPlants;

    public WeatherReport(String city, Double currentTempC, Integer weatherCode,
                         DailyWeather daily, boolean fromCache, long fetchedAt,
                         int adjustedPlants) {
        this.city = city;
        this.currentTempC = currentTempC;
        this.weatherCode = weatherCode;
        this.daily = daily;
        this.fromCache = fromCache;
        this.fetchedAt = fetchedAt;
        this.adjustedPlants = adjustedPlants;
    }
}
