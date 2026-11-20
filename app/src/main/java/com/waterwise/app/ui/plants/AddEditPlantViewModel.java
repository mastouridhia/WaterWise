package com.waterwise.app.ui.plants;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.waterwise.app.data.Plant;
import com.waterwise.app.data.PlantRepository;

public class AddEditPlantViewModel extends AndroidViewModel {

    private final PlantRepository repository;

    public AddEditPlantViewModel(@NonNull Application application) {
        super(application);
        repository = new PlantRepository(application);
    }

    public LiveData<Plant> getPlant(long id) {
        return repository.getPlant(id);
    }

    public void save(Plant plant) {
        repository.save(plant);
    }
}
