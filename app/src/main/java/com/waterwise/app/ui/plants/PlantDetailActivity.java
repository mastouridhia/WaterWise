package com.waterwise.app.ui.plants;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.waterwise.app.R;
import com.waterwise.app.data.Plant;
import com.waterwise.app.databinding.ActivityPlantDetailBinding;
import com.waterwise.app.databinding.ItemInfoRowBinding;
import com.waterwise.app.util.EdgeToEdgeUtil;
import com.waterwise.app.util.PlantImages;
import com.waterwise.app.util.WateringStatus;

/** Shows one plant: info, "Water now", history, edit and delete. */
public class PlantDetailActivity extends AppCompatActivity {

    private static final String EXTRA_PLANT_ID = "plant_id";

    private ActivityPlantDetailBinding binding;
    private PlantDetailViewModel viewModel;
    private Plant plant;

    public static Intent newIntent(Context context, long plantId) {
        return new Intent(context, PlantDetailActivity.class).putExtra(EXTRA_PLANT_ID, plantId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityPlantDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        EdgeToEdgeUtil.padForSystemBars(binding.mainRoot);

        long plantId = getIntent().getLongExtra(EXTRA_PLANT_ID, 0);
        viewModel = new ViewModelProvider(this).get(PlantDetailViewModel.class);
        viewModel.init(plantId);

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (plant == null) return false;
            if (item.getItemId() == R.id.action_edit) {
                startActivity(AddEditPlantActivity.editIntent(this, plant.id));
                return true;
            } else if (item.getItemId() == R.id.action_delete) {
                confirmDelete();
                return true;
            }
            return false;
        });

        WateringLogAdapter historyAdapter = new WateringLogAdapter();
        binding.history.setAdapter(historyAdapter);

        viewModel.getPlant().observe(this, p -> {
            if (p == null) {
                finish(); // the plant was deleted
                return;
            }
            plant = p;
            show(p);
        });
        viewModel.getHistory().observe(this, logs -> {
            historyAdapter.submitList(logs);
            binding.historyEmpty.setVisibility(logs.isEmpty() ? View.VISIBLE : View.GONE);
        });

        binding.buttonWater.setOnClickListener(v -> {
            if (plant == null) return;
            viewModel.waterNow(plant);
            Snackbar.make(binding.getRoot(), getString(R.string.watered_snackbar, plant.nickname),
                    Snackbar.LENGTH_SHORT).setAnchorView(binding.buttonWater).show();
        });
    }

    private void show(Plant p) {
        binding.toolbar.setTitle(p.nickname);
        binding.nickname.setText(p.nickname);
        binding.species.setText(p.speciesName);
        binding.species.setVisibility(p.speciesName == null ? View.GONE : View.VISIBLE);
        binding.status.setText(WateringStatus.label(this, p.nextWateringAt));
        binding.status.setTextColor(WateringStatus.color(this, p.nextWateringAt));

        PlantImages.load(binding.image, p.imageUri);

        setRow(binding.rowLocation, R.string.detail_location,
                getString(p.outdoor ? R.string.outdoor : R.string.indoor));
        setRow(binding.rowFrequency, R.string.detail_frequency,
                getResources().getQuantityString(R.plurals.every_n_days,
                        p.wateringIntervalDays, p.wateringIntervalDays));
        setRow(binding.rowSunlight, R.string.detail_sunlight,
                p.sunlight != null ? p.sunlight : getString(R.string.unknown));
        setRow(binding.rowLastWatered, R.string.detail_last_watered,
                p.lastWateredAt > 0 ? WateringStatus.formatDate(p.lastWateredAt)
                        : getString(R.string.never));
        setRow(binding.rowNextWatering, R.string.detail_next_watering,
                WateringStatus.formatDate(p.nextWateringAt));

        boolean hasNotes = p.notes != null && !p.notes.isEmpty();
        binding.notesTitle.setVisibility(hasNotes ? View.VISIBLE : View.GONE);
        binding.notes.setVisibility(hasNotes ? View.VISIBLE : View.GONE);
        binding.notes.setText(p.notes);
    }

    private void setRow(ItemInfoRowBinding row, int label, String value) {
        row.label.setText(label);
        row.value.setText(value);
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_confirm_title)
                .setMessage(getString(R.string.delete_confirm_message, plant.nickname))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    viewModel.delete(plant);
                    finish();
                })
                .show();
    }
}
