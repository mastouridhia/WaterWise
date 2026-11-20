package com.waterwise.app.ui.plants;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.waterwise.app.R;
import com.waterwise.app.data.WateringLog;
import com.waterwise.app.databinding.ItemLogBinding;
import com.waterwise.app.util.WateringStatus;

/** Watering history of one plant. */
public class WateringLogAdapter extends ListAdapter<WateringLog, WateringLogAdapter.ViewHolder> {

    public WateringLogAdapter() {
        super(DIFF);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemLogBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemLogBinding b;

        ViewHolder(ItemLogBinding binding) {
            super(binding.getRoot());
            b = binding;
        }

        void bind(WateringLog log) {
            int title;
            switch (log.type) {
                case WateringLog.TYPE_SKIPPED_RAIN:
                    title = R.string.log_skipped_rain;
                    break;
                case WateringLog.TYPE_ADVANCED_HEAT:
                    title = R.string.log_advanced_heat;
                    break;
                default:
                    title = R.string.log_watered;
            }
            b.title.setText(title);
            b.date.setText(WateringStatus.formatDate(log.timestamp));
            b.detail.setText(log.detail);
            b.detail.setVisibility(log.detail == null ? View.GONE : View.VISIBLE);
        }
    }

    private static final DiffUtil.ItemCallback<WateringLog> DIFF =
            new DiffUtil.ItemCallback<WateringLog>() {
                @Override
                public boolean areItemsTheSame(@NonNull WateringLog a, @NonNull WateringLog b) {
                    return a.id == b.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull WateringLog a, @NonNull WateringLog b) {
                    return a.timestamp == b.timestamp && a.type.equals(b.type);
                }
            };
}
