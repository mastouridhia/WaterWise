package com.waterwise.app.ui.species;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.waterwise.app.data.SpeciesCache;
import com.waterwise.app.databinding.ActivitySpeciesSearchBinding;
import com.waterwise.app.util.EdgeToEdgeUtil;

/**
 * Searches plant species with the Perenual API and returns the chosen one
 * to the calling screen (AddEditPlantActivity).
 */
public class SpeciesSearchActivity extends AppCompatActivity {

    public static final String EXTRA_QUERY = "query";
    public static final String RESULT_SPECIES_ID = "species_id";
    public static final String RESULT_SPECIES_NAME = "species_name";

    private ActivitySpeciesSearchBinding binding;
    private SpeciesSearchViewModel viewModel;

    public static Intent newIntent(Context context, String query) {
        return new Intent(context, SpeciesSearchActivity.class).putExtra(EXTRA_QUERY, query);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivitySpeciesSearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        EdgeToEdgeUtil.padForSystemBars(binding.mainRoot);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        viewModel = new ViewModelProvider(this).get(SpeciesSearchViewModel.class);

        SpeciesAdapter adapter = new SpeciesAdapter(this::returnSpecies);
        binding.results.setAdapter(adapter);

        viewModel.isLoading().observe(this, loading ->
                binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE));
        viewModel.getResults().observe(this, adapter::submitList);
        viewModel.getMessage().observe(this, res -> {
            binding.message.setVisibility(res == 0 ? View.GONE : View.VISIBLE);
            if (res != 0) binding.message.setText(res);
        });

        // Search only when the user validates (keyboard "search" key or the
        // search icon), not on every letter: this saves API requests.
        binding.inputQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                runSearch();
                return true;
            }
            return false;
        });
        binding.layoutQuery.setEndIconOnClickListener(v -> runSearch());

        if (savedInstanceState == null) {
            String query = getIntent().getStringExtra(EXTRA_QUERY);
            if (query != null && !query.trim().isEmpty()) {
                binding.inputQuery.setText(query.trim());
                runSearch();
            } else {
                binding.inputQuery.requestFocus();
            }
        }
    }

    private void runSearch() {
        CharSequence text = binding.inputQuery.getText();
        String query = text == null ? "" : text.toString().trim();
        hideKeyboard();
        viewModel.search(query);
    }

    private void returnSpecies(SpeciesCache species) {
        Intent data = new Intent()
                .putExtra(RESULT_SPECIES_ID, species.apiId)
                .putExtra(RESULT_SPECIES_NAME,
                        species.commonName != null ? species.commonName : species.scientificName);
        setResult(RESULT_OK, data);
        finish();
    }

    private void hideKeyboard() {
        InputMethodManager imm = getSystemService(InputMethodManager.class);
        if (imm != null) imm.hideSoftInputFromWindow(binding.inputQuery.getWindowToken(), 0);
    }
}
