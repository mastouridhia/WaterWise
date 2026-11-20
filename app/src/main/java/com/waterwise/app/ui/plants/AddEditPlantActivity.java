package com.waterwise.app.ui.plants;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.waterwise.app.R;
import com.waterwise.app.data.Plant;
import com.waterwise.app.databinding.ActivityAddEditPlantBinding;
import com.waterwise.app.util.AppExecutors;
import com.waterwise.app.util.EdgeToEdgeUtil;
import com.waterwise.app.util.PhotoStorage;
import com.waterwise.app.util.WateringStatus;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

/**
 * Form to add a new plant, or edit an existing one when EXTRA_PLANT_ID
 * is given.
 */
public class AddEditPlantActivity extends AppCompatActivity {

    private static final String EXTRA_PLANT_ID = "plant_id";
    private static final String STATE_LAST_WATERED = "last_watered";
    private static final String STATE_PHOTO = "photo";
    private static final String STATE_CAMERA_FILE = "camera_file";

    private ActivityAddEditPlantBinding binding;
    private AddEditPlantViewModel viewModel;

    /** The plant being edited, or null while adding a new one. */
    private Plant plant;
    private long lastWateredAt = System.currentTimeMillis();

    /** Path of the photo shown in the form (saved with the plant). */
    private String photoPath;
    /** File the camera app is writing into, while the camera is open. */
    private File cameraFile;

    // Android Photo Picker: no storage permission needed.
    private final ActivityResultLauncher<PickVisualMediaRequest> pickPhoto =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) importFromGallery(uri);
            });

    // Opens the camera app, which saves the picture into cameraFile.
    private final ActivityResultLauncher<Uri> takePhoto =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraFile != null) {
                    setPhoto(cameraFile.getAbsolutePath());
                } else if (cameraFile != null) {
                    //noinspection ResultOfMethodCallIgnored
                    cameraFile.delete();
                }
                cameraFile = null;
            });

    public static Intent newIntent(Context context) {
        return new Intent(context, AddEditPlantActivity.class);
    }

    public static Intent editIntent(Context context, long plantId) {
        return newIntent(context).putExtra(EXTRA_PLANT_ID, plantId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityAddEditPlantBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        EdgeToEdgeUtil.padForSystemBars(binding.mainRoot);

        viewModel = new ViewModelProvider(this).get(AddEditPlantViewModel.class);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        if (savedInstanceState != null) {
            lastWateredAt = savedInstanceState.getLong(STATE_LAST_WATERED, lastWateredAt);
            photoPath = savedInstanceState.getString(STATE_PHOTO);
            String camera = savedInstanceState.getString(STATE_CAMERA_FILE);
            if (camera != null) cameraFile = new File(camera);
            showPhoto();
        }
        showLastWatered();
        binding.inputLastWatered.setOnClickListener(v -> pickLastWateredDate());

        binding.buttonGallery.setOnClickListener(v -> pickPhoto.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));
        binding.buttonCamera.setOnClickListener(v -> openCamera());

        long plantId = getIntent().getLongExtra(EXTRA_PLANT_ID, 0);
        if (plantId != 0) {
            binding.toolbar.setTitle(R.string.edit_plant);
            viewModel.getPlant(plantId).observe(this, p -> {
                // Fill the form only once, so we don't erase what the user typed.
                if (plant == null && p != null) {
                    plant = p;
                    if (savedInstanceState == null) fillForm(p);
                }
            });
        } else if (savedInstanceState == null) {
            binding.inputInterval.setText("7");
        }

        binding.buttonSave.setOnClickListener(v -> save());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_LAST_WATERED, lastWateredAt);
        outState.putString(STATE_PHOTO, photoPath);
        if (cameraFile != null) outState.putString(STATE_CAMERA_FILE, cameraFile.getAbsolutePath());
    }

    private void openCamera() {
        cameraFile = PhotoStorage.newPhotoFile(this);
        try {
            takePhoto.launch(PhotoStorage.uriForCamera(this, cameraFile));
        } catch (ActivityNotFoundException e) {
            cameraFile = null;
            Toast.makeText(this, R.string.no_camera_app, Toast.LENGTH_SHORT).show();
        }
    }

    /** Copies the chosen picture on a background thread, then shows it. */
    private void importFromGallery(Uri uri) {
        AppExecutors.io().execute(() -> {
            try {
                File copy = PhotoStorage.copyToAppStorage(getApplicationContext(), uri);
                runOnUiThread(() -> setPhoto(copy.getAbsolutePath()));
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, R.string.photo_error,
                        Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void setPhoto(String path) {
        // Delete a photo picked earlier in this form that will not be used.
        if (photoPath != null && (plant == null || !photoPath.equals(plant.imageUri))) {
            PhotoStorage.deleteIfLocal(photoPath);
        }
        photoPath = path;
        showPhoto();
    }

    private void showPhoto() {
        Glide.with(this)
                .load(photoPath)
                .placeholder(R.drawable.ic_plant_placeholder)
                .fallback(R.drawable.ic_plant_placeholder)
                .error(R.drawable.ic_plant_placeholder)
                .centerCrop()
                .into(binding.image);
    }

    private void fillForm(Plant p) {
        binding.inputNickname.setText(p.nickname);
        binding.inputSpecies.setText(p.speciesName);
        binding.inputInterval.setText(String.valueOf(p.wateringIntervalDays));
        binding.inputSunlight.setText(p.sunlight, false);
        binding.switchOutdoor.setChecked(p.outdoor);
        binding.inputNotes.setText(p.notes);
        if (p.lastWateredAt > 0) lastWateredAt = p.lastWateredAt;
        showLastWatered();
        photoPath = p.imageUri;
        showPhoto();
    }

    private void showLastWatered() {
        binding.inputLastWatered.setText(WateringStatus.formatDate(lastWateredAt));
    }

    private void pickLastWateredDate() {
        // The Material date picker works with UTC midnight timestamps.
        long selectionUtc = Instant.ofEpochMilli(lastWateredAt).atZone(ZoneId.systemDefault())
                .toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.pick_last_watered)
                .setSelection(selectionUtc)
                // A plant can't have been watered in the future.
                .setCalendarConstraints(new CalendarConstraints.Builder()
                        .setValidator(DateValidatorPointBackward.now())
                        .build())
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            LocalDate date = Instant.ofEpochMilli(selection).atZone(ZoneOffset.UTC).toLocalDate();
            lastWateredAt = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            showLastWatered();
        });
        picker.show(getSupportFragmentManager(), "last_watered");
    }

    private void save() {
        // Editing, but the plant is not loaded from the database yet.
        if (getIntent().hasExtra(EXTRA_PLANT_ID) && plant == null) return;

        String nickname = text(binding.inputNickname.getText());
        String intervalText = text(binding.inputInterval.getText());

        // Validation
        boolean valid = true;
        binding.layoutNickname.setError(null);
        binding.layoutInterval.setError(null);
        if (nickname.isEmpty()) {
            binding.layoutNickname.setError(getString(R.string.error_required));
            valid = false;
        }
        int interval = TextUtils.isDigitsOnly(intervalText) && !intervalText.isEmpty()
                ? Integer.parseInt(intervalText) : 0;
        if (interval < 1 || interval > 90) {
            binding.layoutInterval.setError(getString(R.string.error_interval));
            valid = false;
        }
        if (!valid) return;

        Plant p = plant != null ? plant : new Plant();
        if (p.id == 0) p.createdAt = System.currentTimeMillis();
        p.nickname = nickname;
        p.speciesName = emptyToNull(text(binding.inputSpecies.getText()));
        p.wateringIntervalDays = interval;
        p.sunlight = emptyToNull(binding.inputSunlight.getText().toString().trim());
        p.outdoor = binding.switchOutdoor.isChecked();
        p.notes = emptyToNull(text(binding.inputNotes.getText()));
        p.lastWateredAt = lastWateredAt;
        p.nextWateringAt = lastWateredAt + TimeUnit.DAYS.toMillis(interval);
        if (!java.util.Objects.equals(p.imageUri, photoPath)) {
            PhotoStorage.deleteIfLocal(p.imageUri); // the old photo was replaced
            p.imageUri = photoPath;
        }

        viewModel.save(p);
        Toast.makeText(this, R.string.plant_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    private static String text(CharSequence s) {
        return s == null ? "" : s.toString().trim();
    }

    private static String emptyToNull(String s) {
        return s.isEmpty() ? null : s;
    }
}
