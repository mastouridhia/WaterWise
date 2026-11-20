package com.waterwise.app.ui.plants;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.waterwise.app.databinding.FragmentPlantsBinding;

/** List of all the user's plants, the most urgent first. */
public class PlantsFragment extends Fragment {

    private FragmentPlantsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPlantsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        PlantAdapter adapter = new PlantAdapter(plant ->
                Toast.makeText(requireContext(), plant.nickname, Toast.LENGTH_SHORT).show());
        binding.recycler.setAdapter(adapter);

        PlantsViewModel viewModel = new ViewModelProvider(this).get(PlantsViewModel.class);
        viewModel.getPlants().observe(getViewLifecycleOwner(), plants -> {
            adapter.submitList(plants);
            binding.textEmpty.setVisibility(plants.isEmpty() ? View.VISIBLE : View.GONE);
        });

        binding.fabAdd.setOnClickListener(v ->
                startActivity(AddEditPlantActivity.newIntent(requireContext())));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
