package com.waterwise.app.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * One event in a plant's watering history. Deleting a plant also deletes
 * its history (CASCADE).
 */
@Entity(tableName = "watering_logs",
        foreignKeys = @ForeignKey(entity = Plant.class,
                parentColumns = "id",
                childColumns = "plantId",
                onDelete = ForeignKey.CASCADE),
        indices = @Index("plantId"))
public class WateringLog {

    public static final String TYPE_WATERED = "WATERED";
    /** Watering postponed because it rained on an outdoor plant. */
    public static final String TYPE_SKIPPED_RAIN = "SKIPPED_RAIN";
    /** Watering moved earlier because of a heat wave. */
    public static final String TYPE_ADVANCED_HEAT = "ADVANCED_HEAT";

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long plantId;

    public long timestamp;

    @NonNull
    public String type = TYPE_WATERED;

    /** Optional human-readable detail, e.g. "Rained 8 mm". */
    public String detail;

    public WateringLog() {
    }

    @Ignore
    public WateringLog(long plantId, long timestamp, @NonNull String type, String detail) {
        this.plantId = plantId;
        this.timestamp = timestamp;
        this.type = type;
        this.detail = detail;
    }
}
