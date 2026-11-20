package com.waterwise.app.ui.today;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.waterwise.app.data.Plant;
import com.waterwise.app.data.PlantRepository;
import com.waterwise.app.weather.WeatherAdvisor;
import com.waterwise.app.weather.WeatherReport;
import com.waterwise.app.weather.WeatherRepository;

import java.util.List;

/** State of the Today screen: weather + plants to water today. */
public class TodayViewModel extends AndroidViewModel {

    private final PlantRepository plantRepository;
    private final WeatherRepository weatherRepository;

    private final LiveData<List<Plant>> duePlants;
    private final MutableLiveData<WeatherReport> weather = new MutableLiveData<>();
    private final MutableLiveData<WeatherRepository.Error> weatherError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public TodayViewModel(@NonNull Application application) {
        super(application);
        plantRepository = new PlantRepository(application);
        weatherRepository = new WeatherRepository(application);
        duePlants = plantRepository.getDueToday();
        refreshWeather();
    }

    public LiveData<List<Plant>> getDuePlants() {
        return duePlants;
    }

    public LiveData<WeatherReport> getWeather() {
        return weather;
    }

    public LiveData<WeatherRepository.Error> getWeatherError() {
        return weatherError;
    }

    public LiveData<Boolean> isLoading() {
        return loading;
    }

    public WeatherAdvisor.Thresholds thresholds() {
        return weatherRepository.thresholds();
    }

    /**
     * Downloads the forecast. Outdoor plants may be adjusted; the due list
     * then updates by itself because it is LiveData from Room.
     */
    public void refreshWeather() {
        loading.setValue(true);
        weatherRepository.refresh(new WeatherRepository.Callback<WeatherReport>() {
            @Override
            public void onResult(WeatherReport report) {
                loading.setValue(false);
                weatherError.setValue(null);
                weather.setValue(report);
            }

            @Override
            public void onError(WeatherRepository.Error error) {
                loading.setValue(false);
                weatherError.setValue(error);
            }
        });
    }

    public void waterNow(Plant plant) {
        plantRepository.waterNow(plant);
    }
}
