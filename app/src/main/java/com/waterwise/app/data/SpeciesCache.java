package com.waterwise.app.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Species returned by the Perenual API, cached locally so that repeated
 * searches work offline and don't use up the free daily request quota.
 */
@Entity(tableName = "species_cache")
public class SpeciesCache {

    /** Perenual species id. */
    @PrimaryKey
    public int apiId;

    public String commonName;
    public String scientificName;

    /** Perenual watering level: "Frequent", "Average", "Minimum" or "None". */
    public String watering;

    public String sunlight;

    /**
     * Days between waterings, computed from the details. Null until the
     * details were downloaded once (added in database version 2).
     */
    public Integer wateringDays;

    /** Small image url for lists (signed url, expires after 24 h). */
    public String imageUrl;
    public long cachedAt;
}
