package com.waterwise.app.ui.weather;

import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.waterwise.app.R;
import com.waterwise.app.databinding.DialogCitySearchBinding;
import com.waterwise.app.network.model.GeocodingResponse;
import com.waterwise.app.util.AppSettings;
import com.waterwise.app.weather.WeatherRepository;

import java.util.List;

/**
 * Two small dialogs: 1) type a city name, 2) choose the right match
 * (e.g. "Tunis, Tunisia" vs "Tunis, Indonesia"). The choice is saved
 * in AppSettings.
 */
public final class CityPicker {

    public interface OnCityChosen {
        void onCityChosen(String name);
    }

    private CityPicker() {
    }

    public static void show(Fragment host, OnCityChosen listener) {
        DialogCitySearchBinding binding =
                DialogCitySearchBinding.inflate(LayoutInflater.from(host.requireContext()));
        String current = new AppSettings(host.requireContext()).getCityName();
        if (current != null) binding.inputCity.setText(current);

        new MaterialAlertDialogBuilder(host.requireContext())
                .setTitle(R.string.city_title)
                .setView(binding.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.city_search, (d, w) -> {
                    CharSequence text = binding.inputCity.getText();
                    String name = text == null ? "" : text.toString().trim();
                    if (name.length() >= 2) search(host, name, listener);
                })
                .show();
    }

    private static void search(Fragment host, String name, OnCityChosen listener) {
        Toast.makeText(host.requireContext(), R.string.city_searching, Toast.LENGTH_SHORT).show();
        new WeatherRepository(host.requireContext()).searchCity(name,
                new WeatherRepository.Callback<List<GeocodingResponse.Place>>() {
                    @Override
                    public void onResult(List<GeocodingResponse.Place> places) {
                        if (!host.isAdded()) return;
                        if (places.isEmpty()) {
                            Toast.makeText(host.requireContext(), R.string.city_not_found,
                                    Toast.LENGTH_LONG).show();
                        } else {
                            choose(host, places, listener);
                        }
                    }

                    @Override
                    public void onError(WeatherRepository.Error error) {
                        if (!host.isAdded()) return;
                        Toast.makeText(host.requireContext(), R.string.error_offline,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private static void choose(Fragment host, List<GeocodingResponse.Place> places,
                               OnCityChosen listener) {
        String[] labels = new String[places.size()];
        for (int i = 0; i < places.size(); i++) labels[i] = places.get(i).label();

        new MaterialAlertDialogBuilder(host.requireContext())
                .setTitle(R.string.city_choose)
                .setItems(labels, (d, which) -> {
                    GeocodingResponse.Place p = places.get(which);
                    new AppSettings(host.requireContext()).setCity(p.name, p.latitude, p.longitude);
                    listener.onCityChosen(p.name);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
