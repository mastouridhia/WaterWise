package com.waterwise.app.network;

import com.waterwise.app.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Creates the HTTP client and the Retrofit API objects (one instance each). */
public final class ApiClient {

    private static OkHttpClient httpClient;
    private static PerenualApi perenualApi;

    private ApiClient() {
    }

    /** Shared HTTP client (also used to download images). */
    public static synchronized OkHttpClient httpClient() {
        if (httpClient == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            // Log requests in Logcat only in debug builds.
            logging.setLevel(BuildConfig.DEBUG
                    ? HttpLoggingInterceptor.Level.BASIC
                    : HttpLoggingInterceptor.Level.NONE);

            httpClient = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .addInterceptor(logging)
                    .build();
        }
        return httpClient;
    }

    public static synchronized PerenualApi perenual() {
        if (perenualApi == null) {
            // Adds "?key=..." to every Perenual request, so the key is
            // written in one place only.
            OkHttpClient client = httpClient().newBuilder()
                    .addInterceptor(chain -> {
                        Request request = chain.request();
                        HttpUrl url = request.url().newBuilder()
                                .addQueryParameter("key", BuildConfig.PERENUAL_API_KEY)
                                .build();
                        return chain.proceed(request.newBuilder().url(url).build());
                    })
                    .build();

            perenualApi = new Retrofit.Builder()
                    .baseUrl(PerenualApi.BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(PerenualApi.class);
        }
        return perenualApi;
    }
}
