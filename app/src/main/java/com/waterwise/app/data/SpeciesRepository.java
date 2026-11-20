package com.waterwise.app.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.waterwise.app.network.ApiClient;
import com.waterwise.app.network.PerenualApi;
import com.waterwise.app.network.model.SpeciesDetailsDto;
import com.waterwise.app.network.model.SpeciesDto;
import com.waterwise.app.network.model.SpeciesListResponse;
import com.waterwise.app.util.AppExecutors;
import com.waterwise.app.util.CareMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Species data from the Perenual API, with a local cache in Room:
 * - every search result is saved, so the same search works offline;
 * - care details are downloaded once per species, which saves the free
 *   daily request quota.
 * Results are delivered on the main thread through a Callback.
 */
public class SpeciesRepository {

    public enum Error {
        /** No internet connection or server not reachable. */
        OFFLINE,
        /** Care details are not included in the free plan for this species. */
        LOCKED,
        /** Too many requests today (free plan limit). */
        QUOTA,
        UNKNOWN
    }

    public interface Callback<T> {
        void onResult(T result);

        void onError(Error error);
    }

    public static class SearchResult {
        public final List<SpeciesCache> species;
        /** True when the API failed and the results come from the cache. */
        public final boolean fromCache;

        SearchResult(List<SpeciesCache> species, boolean fromCache) {
            this.species = species;
            this.fromCache = fromCache;
        }
    }

    /** What we need to fill the plant form. */
    public static class CareInfo {
        public final int wateringDays;
        public final String wateringLevel;
        public final String sunlight;
        /** Fresh image url to download, or null. */
        public final String imageUrl;

        CareInfo(int wateringDays, String wateringLevel, String sunlight, String imageUrl) {
            this.wateringDays = wateringDays;
            this.wateringLevel = wateringLevel;
            this.sunlight = sunlight;
            this.imageUrl = imageUrl;
        }
    }

    private final SpeciesDao dao;
    private final PerenualApi api;
    private final Handler mainThread = new Handler(Looper.getMainLooper());

    public SpeciesRepository(Context context) {
        dao = AppDatabase.get(context).speciesDao();
        api = ApiClient.perenual();
    }

    /** Searches species by name; falls back to the cache if the API fails. */
    public void search(String query, Callback<SearchResult> callback) {
        // enqueue() runs the request in the background and calls us back
        // on the main thread.
        api.searchSpecies(query).enqueue(new retrofit2.Callback<SpeciesListResponse>() {
            @Override
            public void onResponse(@NonNull Call<SpeciesListResponse> call,
                                   @NonNull Response<SpeciesListResponse> response) {
                SpeciesListResponse body = response.body();
                if (!response.isSuccessful() || body == null) {
                    searchInCache(query, callback, errorForHttpCode(response.code(), false));
                    return;
                }
                List<SpeciesDto> dtos = body.data != null ? body.data : Collections.emptyList();
                AppExecutors.io().execute(() -> {
                    List<SpeciesCache> list = saveSearchResults(dtos);
                    mainThread.post(() -> callback.onResult(new SearchResult(list, false)));
                });
            }

            @Override
            public void onFailure(@NonNull Call<SpeciesListResponse> call, @NonNull Throwable t) {
                searchInCache(query, callback, Error.OFFLINE);
            }
        });
    }

    /** Care details of one species: from the cache if known, else from the API. */
    public void getCareInfo(int apiId, Callback<CareInfo> callback) {
        AppExecutors.io().execute(() -> {
            SpeciesCache cached = dao.getById(apiId);
            if (cached != null && cached.wateringDays != null) {
                // The cached image url may have expired (24 h): then the
                // download just fails and the leaf placeholder stays.
                CareInfo info = new CareInfo(cached.wateringDays, cached.watering,
                        cached.sunlight, cached.imageUrl);
                mainThread.post(() -> callback.onResult(info));
            } else if (apiId > PerenualApi.FREE_PLAN_MAX_ID) {
                // Don't waste a request: we know the free plan will refuse it.
                mainThread.post(() -> callback.onError(Error.LOCKED));
            } else {
                mainThread.post(() -> downloadCareInfo(apiId, callback));
            }
        });
    }

    private void downloadCareInfo(int apiId, Callback<CareInfo> callback) {
        api.getDetails(apiId).enqueue(new retrofit2.Callback<SpeciesDetailsDto>() {
            @Override
            public void onResponse(@NonNull Call<SpeciesDetailsDto> call,
                                   @NonNull Response<SpeciesDetailsDto> response) {
                SpeciesDetailsDto d = response.body();
                if (!response.isSuccessful() || d == null) {
                    callback.onError(errorForHttpCode(response.code(), true));
                    return;
                }
                String benchmark = d.wateringBenchmark != null
                        ? jsonToText(d.wateringBenchmark.value) : null;
                int days = CareMapper.wateringDays(benchmark, d.watering);
                String sunlight = CareMapper.formatSunlight(jsonToList(d.sunlight));
                String image = d.defaultImage != null ? d.defaultImage.usableRegularUrl() : null;

                AppExecutors.io().execute(() -> saveCareInfo(apiId, d, days, sunlight));
                callback.onResult(new CareInfo(days, d.watering, sunlight, image));
            }

            @Override
            public void onFailure(@NonNull Call<SpeciesDetailsDto> call, @NonNull Throwable t) {
                callback.onError(Error.OFFLINE);
            }
        });
    }

    // ---- Cache helpers (background thread) ----

    private List<SpeciesCache> saveSearchResults(List<SpeciesDto> dtos) {
        long now = System.currentTimeMillis();
        List<SpeciesCache> list = new ArrayList<>();
        for (SpeciesDto dto : dtos) {
            // Keep care details already downloaded for this species.
            SpeciesCache s = dao.getById(dto.id);
            if (s == null) s = new SpeciesCache();
            s.apiId = dto.id;
            s.commonName = CareMapper.capitalize(dto.commonName);
            s.scientificName = dto.firstScientificName();
            s.imageUrl = dto.defaultImage != null ? dto.defaultImage.usableSmallUrl() : null;
            s.cachedAt = now;
            list.add(s);
        }
        dao.insertAll(list);
        return list;
    }

    private void saveCareInfo(int apiId, SpeciesDetailsDto d, int days, String sunlight) {
        SpeciesCache s = dao.getById(apiId);
        if (s == null) {
            s = new SpeciesCache();
            s.apiId = apiId;
            s.commonName = CareMapper.capitalize(d.commonName);
        }
        s.watering = d.watering;
        s.sunlight = sunlight;
        s.wateringDays = days;
        s.cachedAt = System.currentTimeMillis();
        dao.insertAll(Collections.singletonList(s));
    }

    private void searchInCache(String query, Callback<SearchResult> callback, Error error) {
        AppExecutors.io().execute(() -> {
            List<SpeciesCache> list = dao.search(query);
            mainThread.post(() -> {
                if (list.isEmpty()) {
                    callback.onError(error);
                } else {
                    callback.onResult(new SearchResult(list, true));
                }
            });
        });
    }

    private static Error errorForHttpCode(int code, boolean details) {
        // Perenual answers 429 both for "too many requests" and, on the
        // details endpoint, for species not included in the free plan.
        if (code == 429) return details ? Error.LOCKED : Error.QUOTA;
        return Error.UNKNOWN;
    }

    // ---- JSON helpers: accept a string or a list without crashing ----

    private static String jsonToText(JsonElement e) {
        if (e == null || e.isJsonNull()) return null;
        return e.isJsonPrimitive() ? e.getAsString() : e.toString();
    }

    private static List<String> jsonToList(JsonElement e) {
        List<String> out = new ArrayList<>();
        if (e == null || e.isJsonNull()) return out;
        if (e.isJsonArray()) {
            JsonArray array = e.getAsJsonArray();
            for (JsonElement item : array) {
                if (item.isJsonPrimitive()) out.add(item.getAsString());
            }
        } else if (e.isJsonPrimitive()) {
            out.add(e.getAsString());
        }
        return out;
    }
}
