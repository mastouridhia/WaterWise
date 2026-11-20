package com.waterwise.app.ui.plants;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.waterwise.app.data.Plant;
import com.waterwise.app.data.PlantRepository;
import com.waterwise.app.data.WateringLog;

import java.util.List;

public class PlantDetailViewModel extends AndroidViewModel {

    private final PlantRepository repository;
    private LiveData<Plant> plant;
    private LiveData<List<WateringLog>> history;

    public PlantDetailViewModel(@NonNull Application application) {
        super(application);
        repository = new PlantRepository(application);
    }

    /** Must be called once with the id of the plant to show. */
    public void init(long plantId) {
        if (plant != null) return; // already loaded (e.g. after rotation)
        plant = repository.getPlant(plantId);
        history = repository.getHistory(plantId);
    }

    public LiveData<Plant> getPlant() {
        return plant;
    }

    public LiveData<List<WateringLog>> getHistory() {
        return history;
    }

    public void waterNow(Plant p) {
        repository.waterNow(p);
    }

    public void delete(Plant p) {
        repository.delete(p);
    }
}
