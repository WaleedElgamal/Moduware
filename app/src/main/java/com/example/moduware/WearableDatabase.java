package com.example.moduware;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {WearableData.class}, version = 1)
public abstract class WearableDatabase extends RoomDatabase {
    public abstract WearableDataDao wearableDataDao();
}

