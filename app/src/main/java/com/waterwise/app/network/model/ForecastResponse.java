package com.waterwise.app.network.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * JSON answer of the forecast. Daily lists have one value per day:
 * index 0 = yesterday, 1 = today, 2 = tomorrow, 3 = day after.
 * Values are wrappers (Double) because the API may send null.
 */
public class ForecastResponse {

    public Current current;
    public Daily daily;

    public static class Current {
        @SerializedName("temperature_2m")
        public Double temperature;

        @SerializedName("weather_code")
        public Integer weatherCode;
    }

    public static class Daily {
        public List<String> time;

        @SerializedName("weather_code")
        public List<Integer> weatherCode;

        @SerializedName("temperature_2m_max")
        public List<Double> tempMax;

        @SerializedName("temperature_2m_min")
        public List<Double> tempMin;

        @SerializedName("precipitation_sum")
        public List<Double> precipitation;
    }
}
