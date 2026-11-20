package com.waterwise.app.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.concurrent.TimeUnit;

/** A plant owned by the user. Stored in the "plants" table. */
@Entity(tableName = "plants")
public class Plant {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** Name given by the user, e.g. "Monstera in the living room". */
    @NonNull
    public String nickname = "";

    /** Species common name, e.g. "Swiss cheese plant". */
    public String speciesName;

    /** Species id in the Perenual API, null if the plant was entered manually. */
    public Integer apiSpeciesId;

    /** Local photo (content:// uri) or remote species image url. */
    public String imageUri;

    /** Outdoor plants get rain/heat/frost weather adjustments. */
    public boolean outdoor;

    /** Base watering frequency in days. */
    public int wateringIntervalDays = 7;

    /** Sunlight need, e.g. "full sun", "part shade". */
    public String sunlight;

    public String notes;

    /** Timestamps in milliseconds since epoch. */
    public long lastWateredAt;
    public long nextWateringAt;
    public long createdAt;

    /** Records a watering now and schedules the next one from the base interval. */
    public void markWatered(long now) {
        lastWateredAt = now;
        nextWateringAt = now + TimeUnit.DAYS.toMillis(wateringIntervalDays);
    }
}
