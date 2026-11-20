package com.waterwise.app.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * The app's SQLite database (through Room). There is a single instance for
 * the whole app (singleton).
 */
@Database(entities = {Plant.class, WateringLog.class, SpeciesCache.class},
        version = 2,
        exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    /**
     * Version 1 → 2: species_cache gets a wateringDays column. A migration
     * keeps the user's plants instead of wiping the database.
     */
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE species_cache ADD COLUMN wateringDays INTEGER");
        }
    };

    public abstract PlantDao plantDao();

    public abstract WateringLogDao wateringLogDao();

    public abstract SpeciesDao speciesDao();

    public static AppDatabase get(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "waterwise.db")
                            .addMigrations(MIGRATION_1_2)
                            .build();
                }
            }
        }
        return instance;
    }
}
