package com.waterwise.app.network;

import com.waterwise.app.network.model.ForecastResponse;
import com.waterwise.app.network.model.GeocodingResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Open-Meteo weather APIs (https://open-meteo.com). Free, no API key.
 * Geocoding and forecast use two different hosts, so there are two
 * interfaces, each with its own Retrofit instance in ApiClient.
 */
public final class OpenMeteoApi {

    public static final String GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/";
    public static final String FORECAST_URL = "https://api.open-meteo.com/v1/";

    private OpenMeteoApi() {
    }

    /** City name → coordinates, e.g. GET search?name=Tunis */
    public interface Geocoding {
        @GET("search?count=5&language=en&format=json")
        Call<GeocodingResponse> search(@Query("name") String name);
    }

    /**
     * Current weather + 4 days of daily values: yesterday (past_days=1),
     * today, tomorrow and the day after.
     */
    public interface Forecast {
        @GET("forecast?current=temperature_2m,weather_code"
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum"
                + "&past_days=1&forecast_days=3&timezone=auto")
        Call<ForecastResponse> forecast(@Query("latitude") double latitude,
                                        @Query("longitude") double longitude);
    }
}
