package com.waterwise.app.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface WateringLogDao {

    @Insert
    long insert(WateringLog log);

    /** History of one plant, newest first. */
    @Query("SELECT * FROM watering_logs WHERE plantId = :plantId ORDER BY timestamp DESC")
    LiveData<List<WateringLog>> getForPlant(long plantId);

    /** Number of real waterings since a given time (used for stats). */
    @Query("SELECT COUNT(*) FROM watering_logs WHERE type = 'WATERED' AND timestamp >= :since")
    LiveData<Integer> countWateredSince(long since);
}
