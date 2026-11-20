package com.waterwise.app.network.model;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

/**
 * JSON answer of GET species/details/{id}. Only the fields we use are
 * declared; Gson ignores the others.
 */
public class SpeciesDetailsDto {

    public int id;

    @SerializedName("common_name")
    public String commonName;

    /** "Frequent", "Average", "Minimum" or "None". */
    public String watering;

    /**
     * Usually a list like ["full sun", "part shade"]. Kept as a JsonElement
     * so an unexpected format can't crash the parsing.
     */
    public JsonElement sunlight;

    /** e.g. { "value": "\"7-10\"", "unit": "days" } */
    @SerializedName("watering_general_benchmark")
    public Benchmark wateringBenchmark;

    public Boolean indoor;

    @SerializedName("default_image")
    public ImageDto defaultImage;

    public static class Benchmark {
        public JsonElement value;
        public String unit;
    }
}
