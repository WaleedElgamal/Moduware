package com.example.moduware;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.room.Room;

import java.io.IOException;
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Responsible for processing data streams
 */

public class DataHandler {
    private WearableDatabase db;
    private WearableDataDao wearableDataDao;
    private Context context;

    public DataHandler(Context context) {
        this.context = context;
        db = Room.databaseBuilder(context.getApplicationContext(),
                        WearableDatabase.class, "wearable_database")
                .fallbackToDestructiveMigration()
                .build();
        wearableDataDao = db.wearableDataDao();
    }

    public void saveData(WearableData data) {
        new Thread(() -> {
            wearableDataDao.insert(data);

        }).start();
    }

    public List<WearableData> getAllData() {
        return wearableDataDao.getAllData();
    }

    @SuppressLint("NewApi")
    public void exportDataToFile(String exportType, String sensorAddress) {
        new Thread(() -> {
            List<WearableData> allData = wearableDataDao.getSensorData(sensorAddress);
            if (allData == null || allData.isEmpty()) {
                return;
            }

            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.getDefault()).format(new Date());
            String fileName = exportType + timestamp + ".csv";

            ContentResolver resolver = context.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
            if(uri!=null){
                try (OutputStream outputStream = resolver.openOutputStream(uri);
                     BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
                    writer.write("sensor_address, sensor_name, measurement_type, value, unit, timestamp\n");
                    for (WearableData data : allData) {
                        writer.write(String.format("%s,%s,%s,%s,%s,%s\n",
                                data.getSensorAddress(),
                                data.getSensorName(),
                                data.getMeasurementType(),
                                data.getValue(),
                                data.getUnit(),
                                data.getTimestamp()));
                    }
                    Log.i("Export", "File exported to: " + uri.toString());
                    showToast("File saved in downloads");

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("Export", "Error writing file: " + e.getMessage());
                }
            }
        }).start();
    }

    private void showToast(String message) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        );
    }
}
