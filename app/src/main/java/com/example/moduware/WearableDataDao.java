package com.example.moduware;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface WearableDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(WearableData wearableData);

    @Query("SELECT * FROM wearable_data")
    List<WearableData> getAllData();

    @Query("SELECT * FROM wearable_data WHERE sensorAddress = :sensorAddress")
    List<WearableData> getSensorData(String sensorAddress);
}
