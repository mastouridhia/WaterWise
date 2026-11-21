package com.waterwise.app.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.waterwise.app.BuildConfig;
import com.waterwise.app.R;
import com.waterwise.app.databinding.FragmentSettingsBinding;
import com.waterwise.app.notifications.Notifications;
import com.waterwise.app.notifications.ReminderScheduler;
import com.waterwise.app.ui.weather.CityPicker;
import com.waterwise.app.util.AppSettings;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/** City, daily reminder (on/off + time) and weather rule thresholds. */
public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private AppSettings settings;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        settings = new AppSettings(requireContext());

        // City
        binding.buttonCity.setOnClickListener(v -> CityPicker.show(this, name -> showCity()));

        // Daily reminder
        binding.switchReminder.setChecked(settings.isReminderEnabled());
        binding.switchReminder.setOnCheckedChangeListener((b, checked) -> {
            settings.setReminderEnabled(checked);
            ReminderScheduler.schedule(requireContext());
            showReminder();
        });
        binding.buttonTime.setOnClickListener(v -> pickTime());
        binding.buttonTest.setOnClickListener(v -> {
            ReminderScheduler.runNow(requireContext());
            Toast.makeText(requireContext(), R.string.settings_test_sent, Toast.LENGTH_LONG).show();
        });
        binding.buttonOpenNotifSettings.setOnClickListener(v -> startActivity(
                new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName())));

        // Weather rules: each slider saves its value immediately.
        binding.sliderRain.setValue(Math.round(settings.getRainThresholdMm()));
        binding.sliderHeat.setValue(Math.round(settings.getHeatThresholdC()));
        binding.sliderFrost.setValue(Math.round(settings.getFrostThresholdC()));
        binding.sliderRain.addOnChangeListener((s, value, fromUser) -> {
            settings.setRainThresholdMm(value);
            showRuleLabels();
        });
        binding.sliderHeat.addOnChangeListener((s, value, fromUser) -> {
            settings.setHeatThresholdC(value);
            showRuleLabels();
        });
        binding.sliderFrost.addOnChangeListener((s, value, fromUser) -> {
            settings.setFrostThresholdC(value);
            showRuleLabels();
        });
        binding.buttonResetRules.setOnClickListener(v -> {
            binding.sliderRain.setValue(AppSettings.DEFAULT_RAIN_MM);
            binding.sliderHeat.setValue(AppSettings.DEFAULT_HEAT_C);
            binding.sliderFrost.setValue(AppSettings.DEFAULT_FROST_C);
        });
        showRuleLabels();

        binding.textAbout.setText(getString(R.string.settings_about, BuildConfig.VERSION_NAME));
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refreshed here because the user may come back from the system
        // settings after allowing notifications, or after changing the city.
        showCity();
        showReminder();
    }

    private void showCity() {
        String city = settings.getCityName();
        binding.textCity.setText(city != null
                ? getString(R.string.settings_city, city)
                : getString(R.string.settings_city_none));
    }

    private void showReminder() {
        boolean on = settings.isReminderEnabled();
        LocalTime time = LocalTime.of(settings.getReminderHour(), settings.getReminderMinute());
        binding.textReminderTime.setText(on
                ? getString(R.string.settings_reminder_time,
                time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)))
                : getString(R.string.settings_reminder_off));
        binding.buttonTime.setEnabled(on);
        binding.buttonTest.setEnabled(on);

        boolean blocked = on && !Notifications.canPost(requireContext());
        binding.textPermission.setVisibility(blocked ? View.VISIBLE : View.GONE);
        binding.buttonOpenNotifSettings.setVisibility(blocked ? View.VISIBLE : View.GONE);
    }

    private void pickTime() {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(DateFormat.is24HourFormat(requireContext())
                        ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H)
                .setHour(settings.getReminderHour())
                .setMinute(settings.getReminderMinute())
                .setTitleText(R.string.settings_pick_time)
                .build();
        picker.addOnPositiveButtonClickListener(v -> {
            settings.setReminderTime(picker.getHour(), picker.getMinute());
            ReminderScheduler.schedule(requireContext());
            showReminder();
        });
        picker.show(getChildFragmentManager(), "reminder_time");
    }

    private void showRuleLabels() {
        binding.labelRain.setText(getString(R.string.settings_rain_label,
                format(binding.sliderRain.getValue())));
        binding.labelHeat.setText(getString(R.string.settings_heat_label,
                format(binding.sliderHeat.getValue())));
        binding.labelFrost.setText(getString(R.string.settings_frost_label,
                format(binding.sliderFrost.getValue())));
    }

    private static String format(float value) {
        return String.format(Locale.getDefault(), "%.0f", value);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
