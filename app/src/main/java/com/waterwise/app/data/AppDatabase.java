package com.waterwise.app.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * The app's SQLite database (through Room). There is a single instance for
 * the whole app (singleton).
 */
@Database(entities = {Plant.class, WateringLog.class, SpeciesCache.class},
        version = 1,
        exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract PlantDao plantDao();

    public abstract WateringLogDao wateringLogDao();

    public abstract SpeciesDao speciesDao();

    public static AppDatabase get(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "waterwise.db")
                            .build();
                }
            }
        }
        return instance;
    }
}
