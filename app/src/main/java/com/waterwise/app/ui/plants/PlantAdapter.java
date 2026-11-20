package com.waterwise.app.ui.plants;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.waterwise.app.R;
import com.waterwise.app.data.Plant;
import com.waterwise.app.databinding.ItemPlantBinding;
import com.waterwise.app.util.PlantImages;
import com.waterwise.app.util.WateringStatus;

/**
 * Shows plants in a RecyclerView. ListAdapter + DiffUtil only redraw the
 * rows that actually changed when the list is updated.
 */
public class PlantAdapter extends ListAdapter<Plant, PlantAdapter.ViewHolder> {

    public interface OnPlantClickListener {
        void onPlantClick(Plant plant);
    }

    private final OnPlantClickListener listener;

    public PlantAdapter(OnPlantClickListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPlantBinding binding = ItemPlantBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemPlantBinding b;

        ViewHolder(ItemPlantBinding binding) {
            super(binding.getRoot());
            b = binding;
        }

        void bind(Plant plant) {
            Context context = b.getRoot().getContext();
            b.nickname.setText(plant.nickname);

            String location = context.getString(plant.outdoor ? R.string.outdoor : R.string.indoor);
            b.species.setText(plant.speciesName == null || plant.speciesName.isEmpty()
                    ? location
                    : context.getString(R.string.species_and_location, plant.speciesName, location));

            int color = WateringStatus.color(context, plant.nextWateringAt);
            b.status.setText(WateringStatus.label(context, plant.nextWateringAt));
            b.status.setTextColor(color);
            b.statusStrip.setBackgroundColor(color);

            PlantImages.load(b.image, plant.imageUri);

            b.getRoot().setOnClickListener(v -> listener.onPlantClick(plant));
        }
    }

    private static final DiffUtil.ItemCallback<Plant> DIFF = new DiffUtil.ItemCallback<Plant>() {
        @Override
        public boolean areItemsTheSame(@NonNull Plant a, @NonNull Plant b) {
            return a.id == b.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Plant a, @NonNull Plant b) {
            return a.nickname.equals(b.nickname)
                    && a.nextWateringAt == b.nextWateringAt
                    && a.outdoor == b.outdoor
                    && java.util.Objects.equals(a.speciesName, b.speciesName)
                    && java.util.Objects.equals(a.imageUri, b.imageUri);
        }
    };
}
