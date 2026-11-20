package com.waterwise.app.util;

import android.content.Context;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Plant photos are copied into the app's private "photos" folder, so they
 * keep working even if the original picture is deleted from the gallery.
 */
public final class PhotoStorage {

    private static final String DIR = "photos";

    private PhotoStorage() {
    }

    /** Creates an empty file for a new photo. */
    public static File newPhotoFile(Context context) {
        File dir = new File(context.getFilesDir(), DIR);
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        return new File(dir, "plant_" + System.currentTimeMillis() + ".jpg");
    }

    /** content:// uri that lets the camera app write into our private file. */
    public static Uri uriForCamera(Context context, File file) {
        return FileProvider.getUriForFile(context,
                context.getPackageName() + ".fileprovider", file);
    }

    /** Copies a picture chosen in the gallery into our private folder. */
    public static File copyToAppStorage(Context context, Uri source) throws IOException {
        File target = newPhotoFile(context);
        try (InputStream in = context.getContentResolver().openInputStream(source);
             OutputStream out = new FileOutputStream(target)) {
            if (in == null) throw new IOException("Cannot open " + source);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
        return target;
    }

    /** Deletes the file if it is one of our private photos (not a web url). */
    public static void deleteIfLocal(String path) {
        if (path != null && path.startsWith("/") && path.contains("/" + DIR + "/")) {
            //noinspection ResultOfMethodCallIgnored
            new File(path).delete();
        }
    }
}
