package com.example.moduware;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.List;

@Entity(tableName = "wearable_data")
public class WearableData {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String sensorAddress;
    private String sensorName;
    private String measurementType;
    private String value;
    private String unit;
    private String timestamp;

    public WearableData(String sensorAddress, String sensorName, String measurementType, String value, String unit, String timestamp) {
        this.sensorAddress = sensorAddress;
        this.sensorName = sensorName;
        this.measurementType = measurementType;
        this.value = value;
        this.unit = unit;
        this.timestamp = timestamp;
    }

    public WearableData(String sensorAddress, String sensorName, List<String> data) {
        this.sensorAddress = sensorAddress;
        this.sensorName = sensorName;
        this.measurementType = data.get(0);
        this.value = data.get(1);
        this.unit = data.get(2);
        this.timestamp = data.get(3);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSensorAddress() {
        return sensorAddress;
    }

    public void setSensorAddress(String sensorAddress) {
        this.sensorAddress = sensorAddress;
    }

    public String getSensorName() {
        return sensorName;
    }

    public void setSensorName(String sensorName) {
        this.sensorName = sensorName;
    }

    public String getMeasurementType() {
        return measurementType;
    }

    public void setMeasurementType(String measurementType) {
        this.measurementType = measurementType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
