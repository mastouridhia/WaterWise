package com.waterwise.app.ui.plants;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.waterwise.app.R;
import com.waterwise.app.data.Plant;
import com.waterwise.app.data.SpeciesRepository;
import com.waterwise.app.databinding.ActivityAddEditPlantBinding;
import com.waterwise.app.ui.species.SpeciesSearchActivity;
import com.waterwise.app.util.AppExecutors;
import com.waterwise.app.util.EdgeToEdgeUtil;
import com.waterwise.app.util.PhotoStorage;
import com.waterwise.app.util.PlantImages;
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
    private static final String STATE_SPECIES_ID = "species_id";

    private ActivityAddEditPlantBinding binding;
    private AddEditPlantViewModel viewModel;
    private SpeciesRepository speciesRepository;

    /** The plant being edited, or null while adding a new one. */
    private Plant plant;
    private long lastWateredAt = System.currentTimeMillis();

    /** Path of the photo shown in the form (saved with the plant). */
    private String photoPath;
    /** File the camera app is writing into, while the camera is open. */
    private File cameraFile;
    /** Perenual id of the species chosen in the search, or null. */
    private Integer speciesId;

    // Opens the species search and receives the chosen species.
    private final ActivityResultLauncher<Intent> searchSpecies =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        Intent data = result.getData();
                        if (result.getResultCode() == RESULT_OK && data != null) {
                            onSpeciesChosen(
                                    data.getIntExtra(SpeciesSearchActivity.RESULT_SPECIES_ID, 0),
                                    data.getStringExtra(SpeciesSearchActivity.RESULT_SPECIES_NAME));
                        }
                    });

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
        speciesRepository = new SpeciesRepository(this);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        if (savedInstanceState != null) {
            lastWateredAt = savedInstanceState.getLong(STATE_LAST_WATERED, lastWateredAt);
            photoPath = savedInstanceState.getString(STATE_PHOTO);
            String camera = savedInstanceState.getString(STATE_CAMERA_FILE);
            if (camera != null) cameraFile = new File(camera);
            if (savedInstanceState.containsKey(STATE_SPECIES_ID)) {
                speciesId = savedInstanceState.getInt(STATE_SPECIES_ID);
            }
        }
        showPhoto();
        showLastWatered();
        binding.inputLastWatered.setOnClickListener(v -> pickLastWateredDate());

        binding.buttonGallery.setOnClickListener(v -> pickPhoto.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));
        binding.buttonCamera.setOnClickListener(v -> openCamera());
        binding.layoutSpecies.setEndIconOnClickListener(v -> searchSpecies.launch(
                SpeciesSearchActivity.newIntent(this, text(binding.inputSpecies.getText()))));

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
        if (speciesId != null) outState.putInt(STATE_SPECIES_ID, speciesId);
    }

    /**
     * Called when a species was picked in SpeciesSearchActivity: loads its
     * care info (cache or Perenual API) and fills the form.
     */
    private void onSpeciesChosen(int id, String name) {
        speciesId = id;
        binding.inputSpecies.setText(name);
        binding.careProgress.setVisibility(View.VISIBLE);
        binding.careInfo.setVisibility(View.VISIBLE);
        binding.careInfo.setText(R.string.care_loading);

        speciesRepository.getCareInfo(id, new SpeciesRepository.Callback<SpeciesRepository.CareInfo>() {
            @Override
            public void onResult(SpeciesRepository.CareInfo info) {
                if (isDestroyed()) return;
                binding.careProgress.setVisibility(View.GONE);
                binding.inputInterval.setText(String.valueOf(info.wateringDays));
                binding.layoutInterval.setError(null);
                if (info.sunlight != null) {
                    binding.inputSunlight.setText(info.sunlight, false);
                    binding.careInfo.setText(getString(R.string.care_filled,
                            info.wateringDays, info.sunlight));
                } else {
                    binding.careInfo.setText(getString(R.string.care_filled_no_sun,
                            info.wateringDays));
                }
                // Use the species picture if the user has no photo yet.
                if (photoPath == null && info.imageUrl != null) {
                    downloadSpeciesImage(info.imageUrl);
                }
            }

            @Override
            public void onError(SpeciesRepository.Error error) {
                if (isDestroyed()) return;
                binding.careProgress.setVisibility(View.GONE);
                binding.careInfo.setText(error == SpeciesRepository.Error.LOCKED
                        ? R.string.care_manual_message_locked
                        : R.string.care_manual_message_offline);
                askWateringLevel();
            }
        });
    }

    /** Fallback when the API has no care data: the user picks a level. */
    private void askWateringLevel() {
        int[] days = {3, 7, 14};
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.care_manual_title)
                .setItems(R.array.watering_levels, (dialog, which) -> {
                    binding.inputInterval.setText(String.valueOf(days[which]));
                    binding.layoutInterval.setError(null);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void downloadSpeciesImage(String url) {
        AppExecutors.io().execute(() -> {
            try {
                File file = PhotoStorage.download(getApplicationContext(), url);
                runOnUiThread(() -> {
                    // The user may have picked their own photo meanwhile.
                    if (!isDestroyed() && photoPath == null) {
                        setPhoto(file.getAbsolutePath());
                    } else {
                        PhotoStorage.deleteIfLocal(file.getAbsolutePath());
                    }
                });
            } catch (IOException ignored) {
                // No picture is not a problem: the leaf placeholder is shown.
            }
        });
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
        PlantImages.load(binding.image, photoPath);
    }

    private void fillForm(Plant p) {
        binding.inputNickname.setText(p.nickname);
        binding.inputSpecies.setText(p.speciesName);
        speciesId = p.apiSpeciesId;
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
        p.apiSpeciesId = p.speciesName != null ? speciesId : null;
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
