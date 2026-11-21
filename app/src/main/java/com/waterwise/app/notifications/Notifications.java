package com.waterwise.app.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.waterwise.app.MainActivity;
import com.waterwise.app.R;

/** Creates the notification channel and shows the daily reminder. */
public final class Notifications {

    public static final String CHANNEL_WATERING = "watering";
    private static final int ID_DAILY = 1;

    private Notifications() {
    }

    /** Android 8+: notifications must belong to a channel (user can mute it). */
    public static void createChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(CHANNEL_WATERING,
                context.getString(R.string.channel_name), NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.channel_description));
        context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    /** Android 13+ needs the POST_NOTIFICATIONS runtime permission. */
    public static boolean canPost(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    public static void showDaily(Context context, String title, String text) {
        if (!canPost(context)) return;

        // Tapping the notification opens the app on the Today screen.
        Intent intent = new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pending = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_WATERING)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(context, R.color.green_700))
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(pending)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(context).notify(ID_DAILY, builder.build());
        } catch (SecurityException ignored) {
            // Permission removed between the check and the call.
        }
    }
}
