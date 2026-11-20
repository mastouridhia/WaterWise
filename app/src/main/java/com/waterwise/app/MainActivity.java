package com.waterwise.app;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.waterwise.app.databinding.ActivityMainBinding;
import com.waterwise.app.ui.plants.PlantsFragment;
import com.waterwise.app.ui.settings.SettingsFragment;
import com.waterwise.app.ui.today.TodayFragment;

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
        } else {
            binding.toolbar.setTitle(binding.bottomNav.getMenu()
                    .findItem(binding.bottomNav.getSelectedItemId()).getTitle());
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
