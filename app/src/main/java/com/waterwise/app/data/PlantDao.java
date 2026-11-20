package com.waterwise.app.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * SQL queries on the plants table. Methods returning LiveData are observed
 * by the UI and run in the background automatically. The others must be
 * called from a background thread.
 */
@Dao
public interface PlantDao {

    @Insert
    long insert(Plant plant);

    @Update
    void update(Plant plant);

    @Delete
    void delete(Plant plant);

    /** All plants, the most urgent first. */
    @Query("SELECT * FROM plants ORDER BY nextWateringAt ASC")
    LiveData<List<Plant>> getAll();

    @Query("SELECT * FROM plants WHERE id = :id")
    LiveData<Plant> getById(long id);

    @Query("SELECT * FROM plants WHERE id = :id")
    Plant getByIdSync(long id);

    /** Plants whose next watering is before the given time (due or overdue). */
    @Query("SELECT * FROM plants WHERE nextWateringAt <= :before ORDER BY nextWateringAt ASC")
    LiveData<List<Plant>> getDueBefore(long before);

    @Query("SELECT * FROM plants WHERE nextWateringAt <= :before ORDER BY nextWateringAt ASC")
    List<Plant> getDueBeforeSync(long before);

    @Query("SELECT * FROM plants WHERE outdoor = 1")
    List<Plant> getOutdoorSync();
}
