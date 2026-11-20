package com.waterwise.app.data;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.waterwise.app.util.AppExecutors;
import com.waterwise.app.util.PhotoStorage;

import java.util.List;

/**
 * Single entry point for plant data. ViewModels talk to the repository,
 * never to the DAOs directly. Writes run on a background thread.
 */
public class PlantRepository {

    private final PlantDao plantDao;
    private final WateringLogDao logDao;

    public PlantRepository(Context context) {
        AppDatabase db = AppDatabase.get(context);
        plantDao = db.plantDao();
        logDao = db.wateringLogDao();
    }

    public LiveData<List<Plant>> getAllPlants() {
        return plantDao.getAll();
    }

    public LiveData<Plant> getPlant(long id) {
        return plantDao.getById(id);
    }

    public LiveData<List<WateringLog>> getHistory(long plantId) {
        return logDao.getForPlant(plantId);
    }

    /** Inserts a new plant (id == 0) or updates an existing one. */
    public void save(Plant plant) {
        AppExecutors.io().execute(() -> {
            if (plant.id == 0) {
                plant.id = plantDao.insert(plant);
            } else {
                plantDao.update(plant);
            }
        });
    }

    /** Deletes the plant, its history (cascade) and its photo file. */
    public void delete(Plant plant) {
        AppExecutors.io().execute(() -> {
            plantDao.delete(plant);
            PhotoStorage.deleteIfLocal(plant.imageUri);
        });
    }

    /** "Water now": updates the dates and adds an entry to the history. */
    public void waterNow(Plant plant) {
        AppExecutors.io().execute(() -> {
            long now = System.currentTimeMillis();
            plant.markWatered(now);
            plantDao.update(plant);
            logDao.insert(new WateringLog(plant.id, now, WateringLog.TYPE_WATERED, null));
        });
    }
}
