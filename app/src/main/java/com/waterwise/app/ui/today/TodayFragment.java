package com.waterwise.app.ui.today;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.waterwise.app.R;
import com.waterwise.app.databinding.FragmentTodayBinding;
import com.waterwise.app.ui.plants.PlantAdapter;
import com.waterwise.app.ui.plants.PlantDetailActivity;
import com.waterwise.app.ui.weather.CityPicker;
import com.waterwise.app.weather.DailyWeather;
import com.waterwise.app.weather.WeatherAdvisor;
import com.waterwise.app.weather.WeatherCodes;
import com.waterwise.app.weather.WeatherReport;
import com.waterwise.app.weather.WeatherRepository;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/** Weather card + alerts + list of plants that need water today. */
public class TodayFragment extends Fragment {

    private FragmentTodayBinding binding;
    private TodayViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTodayBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(TodayViewModel.class);

        PlantAdapter adapter = new PlantAdapter(
                plant -> startActivity(PlantDetailActivity.newIntent(requireContext(), plant.id)),
                plant -> {
                    viewModel.waterNow(plant);
                    Snackbar.make(binding.getRoot(),
                            getString(R.string.watered_snackbar, plant.nickname),
                            Snackbar.LENGTH_SHORT).show();
                });
        binding.dueList.setAdapter(adapter);

        viewModel.getDuePlants().observe(getViewLifecycleOwner(), plants -> {
            adapter.submitList(plants);
            binding.textEmpty.setVisibility(plants.isEmpty() ? View.VISIBLE : View.GONE);
        });
        viewModel.getWeather().observe(getViewLifecycleOwner(), this::showWeather);
        viewModel.getWeatherError().observe(getViewLifecycleOwner(), this::showWeatherError);
        viewModel.isLoading().observe(getViewLifecycleOwner(),
                loading -> binding.swipeRefresh.setRefreshing(loading));

        binding.swipeRefresh.setOnRefreshListener(viewModel::refreshWeather);
        binding.buttonSetCity.setOnClickListener(v -> pickCity());
        binding.buttonChangeCity.setOnClickListener(v -> pickCity());
    }

    private void pickCity() {
        CityPicker.show(this, name -> viewModel.refreshWeather());
    }

    private void showWeather(WeatherReport report) {
        if (report == null) return;
        binding.groupNoCity.setVisibility(View.GONE);
        binding.weatherError.setVisibility(View.GONE);
        binding.groupWeather.setVisibility(View.VISIBLE);

        DailyWeather d = report.daily;
        binding.weatherEmoji.setText(WeatherCodes.emoji(report.weatherCode));
        binding.weatherCity.setText(report.city);
        String description = getString(WeatherCodes.description(report.weatherCode));
        binding.weatherNow.setText(report.currentTempC == null ? description
                : getString(R.string.weather_now, description, round(report.currentTempC)));
        binding.weatherDetails.setText(getString(R.string.weather_details,
                round(d.recentRainMm()), round(d.maxTodayC), round(d.minTodayC)));

        String time = DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(report.fetchedAt));
        if (report.fromCache) {
            String date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(new Date(report.fetchedAt));
            binding.weatherUpdated.setText(getString(R.string.weather_offline, date));
        } else {
            binding.weatherUpdated.setText(getString(R.string.weather_updated, time));
        }

        // Alerts
        WeatherAdvisor.Thresholds t = viewModel.thresholds();
        showAlert(binding.alertFrost, WeatherAdvisor.frostAlert(d, t),
                getString(R.string.alert_frost, round(d.coldestNext2DaysC())));
        showAlert(binding.alertHeat, WeatherAdvisor.heatAlert(d, t),
                getString(R.string.alert_heat, round(d.hottestNext2DaysC())));
        showAlert(binding.alertRain, WeatherAdvisor.rainInfo(d, t),
                getString(R.string.alert_rain, round(d.recentRainMm())));
        showAlert(binding.alertAdjusted, report.adjustedPlants > 0,
                getResources().getQuantityString(R.plurals.alert_adjusted,
                        report.adjustedPlants, report.adjustedPlants));
    }

    private void showWeatherError(WeatherRepository.Error error) {
        if (error == null) return;
        binding.groupWeather.setVisibility(View.GONE);
        if (error == WeatherRepository.Error.NO_CITY) {
            binding.groupNoCity.setVisibility(View.VISIBLE);
            binding.weatherError.setVisibility(View.GONE);
        } else {
            binding.groupNoCity.setVisibility(View.GONE);
            binding.weatherError.setVisibility(View.VISIBLE);
            binding.weatherError.setText(R.string.weather_unavailable);
        }
    }

    private static void showAlert(View view, boolean visible, String text) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible) ((android.widget.TextView) view).setText(text);
    }

    private static String round(double value) {
        return String.format(Locale.getDefault(), "%.0f", value);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
