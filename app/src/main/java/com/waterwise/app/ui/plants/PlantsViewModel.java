package com.waterwise.app.ui.plants;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.waterwise.app.data.Plant;
import com.waterwise.app.data.PlantRepository;

import java.util.List;

/**
 * Holds the plant list for PlantsFragment. It survives screen rotations,
 * and the LiveData updates the list automatically when the database changes.
 */
public class PlantsViewModel extends AndroidViewModel {

    private final LiveData<List<Plant>> plants;

    public PlantsViewModel(@NonNull Application application) {
        super(application);
        plants = new PlantRepository(application).getAllPlants();
    }

    public LiveData<List<Plant>> getPlants() {
        return plants;
    }
}
