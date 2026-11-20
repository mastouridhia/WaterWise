package com.waterwise.app.network;

import com.waterwise.app.network.model.SpeciesDetailsDto;
import com.waterwise.app.network.model.SpeciesListResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Perenual plant database (https://perenual.com/docs/api).
 * Retrofit generates the implementation of this interface. The API key is
 * added to every request by ApiClient.
 */
public interface PerenualApi {

    String BASE_URL = "https://perenual.com/api/v2/";

    /**
     * Free plan: full care details are only available for species with an
     * id up to this value. Other ids answer HTTP 429 "Please upgrade".
     */
    int FREE_PLAN_MAX_ID = 3000;

    /** Search species by name, e.g. GET species-list?q=aloe */
    @GET("species-list")
    Call<SpeciesListResponse> searchSpecies(@Query("q") String query);

    /** Care details of one species, e.g. GET species/details/728 */
    @GET("species/details/{id}")
    Call<SpeciesDetailsDto> getDetails(@Path("id") int id);
}
