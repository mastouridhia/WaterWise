package com.waterwise.app.ui.species;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.waterwise.app.R;
import com.waterwise.app.data.SpeciesCache;
import com.waterwise.app.databinding.ItemSpeciesBinding;
import com.waterwise.app.network.PerenualApi;
import com.waterwise.app.util.PlantImages;

/** Search results: image, common name, scientific name, care-data badge. */
public class SpeciesAdapter extends ListAdapter<SpeciesCache, SpeciesAdapter.ViewHolder> {

    public interface OnSpeciesClickListener {
        void onSpeciesClick(SpeciesCache species);
    }

    private final OnSpeciesClickListener listener;

    public SpeciesAdapter(OnSpeciesClickListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemSpeciesBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemSpeciesBinding b;

        ViewHolder(ItemSpeciesBinding binding) {
            super(binding.getRoot());
            b = binding;
        }

        void bind(SpeciesCache s) {
            Context context = b.getRoot().getContext();
            b.commonName.setText(s.commonName != null ? s.commonName : s.scientificName);
            b.scientificName.setText(s.scientificName);
            b.scientificName.setVisibility(s.scientificName == null ? View.GONE : View.VISIBLE);

            // Tells the user before clicking whether the form can be auto-filled.
            boolean hasCare = s.wateringDays != null || s.apiId <= PerenualApi.FREE_PLAN_MAX_ID;
            b.careBadge.setText(hasCare ? R.string.care_available : R.string.care_manual);
            b.careBadge.setTextColor(ContextCompat.getColor(context,
                    hasCare ? R.color.status_ok : R.color.status_due));

            PlantImages.load(b.image, s.imageUrl);
            b.getRoot().setOnClickListener(v -> listener.onSpeciesClick(s));
        }
    }

    private static final DiffUtil.ItemCallback<SpeciesCache> DIFF =
            new DiffUtil.ItemCallback<SpeciesCache>() {
                @Override
                public boolean areItemsTheSame(@NonNull SpeciesCache a, @NonNull SpeciesCache b) {
                    return a.apiId == b.apiId;
                }

                @Override
                public boolean areContentsTheSame(@NonNull SpeciesCache a, @NonNull SpeciesCache b) {
                    return java.util.Objects.equals(a.commonName, b.commonName)
                            && java.util.Objects.equals(a.imageUrl, b.imageUrl);
                }
            };
}
