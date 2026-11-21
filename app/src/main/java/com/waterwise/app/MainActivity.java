package com.waterwise.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.waterwise.app.databinding.ActivityMainBinding;
import com.waterwise.app.ui.plants.PlantsFragment;
import com.waterwise.app.ui.settings.SettingsFragment;
import com.waterwise.app.ui.today.TodayFragment;
import com.waterwise.app.util.AppSettings;

/**
 * Hosts the three main screens (Today, My Plants, Settings) and switches
 * between them with the bottom navigation bar.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Keep the toolbar below the status bar. The bottom navigation
        // handles the navigation-bar inset by itself.
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainRoot, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, 0);
            return insets;
        });

        binding.bottomNav.setOnItemSelectedListener(this::onNavItemSelected);

        // On first launch show "Today". After a rotation the FragmentManager
        // restores the current fragment itself.
        if (savedInstanceState == null) {
            binding.bottomNav.setSelectedItemId(R.id.nav_today);
            askNotificationPermission();
        } else {
            binding.toolbar.setTitle(binding.bottomNav.getMenu()
                    .findItem(binding.bottomNav.getSelectedItemId()).getTitle());
        }
    }

    // Result of the system "Allow notifications?" dialog. If refused, the
    // Settings screen explains how to enable them later.
    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            });

    /** Android 13+: notifications need a runtime permission. */
    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && new AppSettings(this).isReminderEnabled()
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private boolean onNavItemSelected(MenuItem item) {
        Fragment fragment;
        int id = item.getItemId();
        if (id == R.id.nav_plants) {
            fragment = new PlantsFragment();
        } else if (id == R.id.nav_settings) {
            fragment = new SettingsFragment();
        } else {
            fragment = new TodayFragment();
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        binding.toolbar.setTitle(item.getTitle());
        return true;
    }
}
