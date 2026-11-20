package com.waterwise.app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SpeciesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<SpeciesCache> species);

    /** Offline search in previously fetched species. */
    @Query("SELECT * FROM species_cache WHERE commonName LIKE '%' || :query || '%' "
            + "OR scientificName LIKE '%' || :query || '%' ORDER BY commonName LIMIT 30")
    List<SpeciesCache> search(String query);

    @Query("SELECT * FROM species_cache WHERE apiId = :apiId")
    SpeciesCache getById(int apiId);
}
