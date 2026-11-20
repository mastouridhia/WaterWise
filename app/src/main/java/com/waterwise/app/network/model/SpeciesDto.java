package com.waterwise.app.network.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** One species in the search results. Gson fills the fields from JSON. */
public class SpeciesDto {

    public int id;

    @SerializedName("common_name")
    public String commonName;

    @SerializedName("scientific_name")
    public List<String> scientificName;

    @SerializedName("default_image")
    public ImageDto defaultImage;

    public String firstScientificName() {
        return scientificName == null || scientificName.isEmpty() ? null : scientificName.get(0);
    }
}
