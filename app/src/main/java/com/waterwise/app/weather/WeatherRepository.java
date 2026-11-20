package com.waterwise.app.weather;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.WorkerThread;

import com.google.gson.Gson;
import com.waterwise.app.R;
import com.waterwise.app.data.AppDatabase;
import com.waterwise.app.data.Plant;
import com.waterwise.app.data.PlantDao;
import com.waterwise.app.data.WateringLog;
import com.waterwise.app.data.WateringLogDao;
import com.waterwise.app.network.ApiClient;
import com.waterwise.app.network.model.ForecastResponse;
import com.waterwise.app.network.model.GeocodingResponse;
import com.waterwise.app.util.AppExecutors;
import com.waterwise.app.util.AppSettings;

import java.io.IOException;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import retrofit2.Response;

/**
 * Weather from Open-Meteo:
 * - finds a city's coordinates (geocoding);
 * - downloads the forecast, saves it for offline use, and applies the
 *   WeatherAdvisor rules to the outdoor plants.
 */
public class WeatherRepository {

    public enum Error {NO_CITY, OFFLINE}

    public interface Callback<T> {
        void onResult(T result);

        void onError(Error error);
    }

    private final Context context;
    private final AppSettings settings;
    private final PlantDao plantDao;
    private final WateringLogDao logDao;
    private final Gson gson = new Gson();
    private final Handler mainThread = new Handler(Looper.getMainLooper());

    public WeatherRepository(Context context) {
        this.context = context.getApplicationContext();
        settings = new AppSettings(context);
        AppDatabase db = AppDatabase.get(context);
        plantDao = db.plantDao();
        logDao = db.wateringLogDao();
    }

    /** City name → list of matching places (empty list if none). */
    public void searchCity(String name, Callback<List<GeocodingResponse.Place>> callback) {
        AppExecutors.io().execute(() -> {
            try {
                Response<GeocodingResponse> response =
                        ApiClient.geocoding().search(name).execute();
                GeocodingResponse body = response.body();
                List<GeocodingResponse.Place> places =
                        body != null && body.results != null ? body.results : Collections.emptyList();
                mainThread.post(() -> callback.onResult(places));
            } catch (IOException e) {
                mainThread.post(() -> callback.onError(Error.OFFLINE));
            }
        });
    }

    /**
     * Refreshes the weather in the background. If the network fails, the
     * last saved forecast is returned (fromCache = true).
     */
    public void refresh(Callback<WeatherReport> callback) {
        AppExecutors.io().execute(() -> {
            if (!settings.hasCity()) {
                mainThread.post(() -> callback.onError(Error.NO_CITY));
                return;
            }
            try {
                WeatherReport report = fetchAndApplyRules();
                mainThread.post(() -> callback.onResult(report));
            } catch (IOException e) {
                WeatherReport cached = loadCachedReport();
                mainThread.post(() -> {
                    if (cached != null) callback.onResult(cached);
                    else callback.onError(Error.OFFLINE);
                });
            }
        });
    }

    /**
     * Downloads the forecast and updates the outdoor plants. Blocking:
     * call it from a background thread (also used by the daily worker).
     */
    @WorkerThread
    public WeatherReport fetchAndApplyRules() throws IOException {
        Response<ForecastResponse> response = ApiClient.forecast()
                .forecast(settings.getLatitude(), settings.getLongitude())
                .execute();
        ForecastResponse body = response.body();
        if (!response.isSuccessful() || body == null || body.daily == null) {
            throw new IOException("Forecast error HTTP " + response.code());
        }
        long now = System.currentTimeMillis();
        settings.setLastForecast(gson.toJson(body), now);

        DailyWeather daily = toDailyWeather(body.daily);
        int adjusted = applyRules(daily, now);
        return toReport(body, daily, false, now, adjusted);
    }

    /** Applies the rain/heat rules to every outdoor plant and logs the changes. */
    @WorkerThread
    private int applyRules(DailyWeather weather, long now) {
        WeatherAdvisor.Thresholds thresholds = thresholds();
        int count = 0;
        for (Plant plant : plantDao.getOutdoorSync()) {
            WeatherAdvisor.Advice advice = WeatherAdvisor.advise(
                    plant, weather, thresholds, now, ZoneId.systemDefault());
            if (advice.action == WeatherAdvisor.Action.NONE) continue;

            plant.lastWateredAt = advice.newLastWateredAt;
            plant.nextWateringAt = advice.newNextWateringAt;
            plantDao.update(plant);

            String type;
            String detail;
            if (advice.action == WeatherAdvisor.Action.POSTPONE_RAIN) {
                type = WateringLog.TYPE_SKIPPED_RAIN;
                detail = context.getString(R.string.log_detail_rain,
                        format(weather.recentRainMm()));
            } else {
                type = WateringLog.TYPE_ADVANCED_HEAT;
                detail = context.getString(R.string.log_detail_heat,
                        format(weather.hottestNext2DaysC()));
            }
            logDao.insert(new WateringLog(plant.id, now, type, detail));
            count++;
        }
        return count;
    }

    public WeatherAdvisor.Thresholds thresholds() {
        return new WeatherAdvisor.Thresholds(settings.getRainThresholdMm(),
                settings.getHeatThresholdC(), settings.getFrostThresholdC());
    }

    /** Last saved forecast, or null. Rules are not applied to old data. */
    @WorkerThread
    private WeatherReport loadCachedReport() {
        String json = settings.getLastForecastJson();
        if (json == null) return null;
        ForecastResponse body = gson.fromJson(json, ForecastResponse.class);
        if (body == null || body.daily == null) return null;
        return toReport(body, toDailyWeather(body.daily), true, settings.getLastForecastAt(), 0);
    }

    private WeatherReport toReport(ForecastResponse body, DailyWeather daily,
                                   boolean fromCache, long time, int adjusted) {
        Double temp = body.current != null ? body.current.temperature : null;
        Integer code = body.current != null ? body.current.weatherCode : null;
        return new WeatherReport(settings.getCityName(), temp, code, daily, fromCache, time, adjusted);
    }

    /** Index 0 = yesterday, 1 = today, 2 = tomorrow (because past_days=1). */
    static DailyWeather toDailyWeather(ForecastResponse.Daily d) {
        return new DailyWeather(
                value(d.precipitation, 0, 0), value(d.precipitation, 1, 0),
                value(d.tempMax, 1, 20), value(d.tempMax, 2, 20),
                value(d.tempMin, 1, 10), value(d.tempMin, 2, 10));
    }

    private static double value(List<Double> list, int index, double fallback) {
        if (list == null || index >= list.size() || list.get(index) == null) return fallback;
        return list.get(index);
    }

    private static String format(double value) {
        return String.format(Locale.US, "%.0f", value);
    }
}
