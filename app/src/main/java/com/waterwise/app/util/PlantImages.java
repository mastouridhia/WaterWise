package com.waterwise.app.util;

import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.waterwise.app.R;

/** Loads a plant photo, or shows a small leaf icon when there is none. */
public final class PlantImages {

    private PlantImages() {
    }

    public static void load(ImageView view, String path) {
        if (path == null) {
            Glide.with(view).clear(view);
            view.setScaleType(ImageView.ScaleType.CENTER);
            view.setImageResource(R.drawable.ic_plant_placeholder);
            return;
        }
        view.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Glide.with(view)
                .load(path)
                .error(R.drawable.ic_plant_placeholder)
                .centerCrop()
                .into(view);
    }
}
