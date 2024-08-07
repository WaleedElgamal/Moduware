package plugins;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CoreParser {

    /**
     * Flag bits for the Core Body Temperature characteristic.
     */
    private static final int FLAG_SKIN_TEMPERATURE = 1; // Bit 0
    private static final int FLAG_CORE_RESERVED = 1 << 1; // Bit 1
    private static final int FLAG_QUALITY_AND_STATE = 1 << 2; // Bit 2
    private static final int FLAG_TEMPERATURE_UNIT = 1 << 3; // Bit 3
    private static final int FLAG_HEART_RATE = 1 << 4; // Bit 4

    /**
     * Flag bits for the Temperature Measurement characteristic.
     */
    private static final int FLAG_TEMPERATURE_UNIT_2 = 1; // Bit 0
    private static final int FLAG_TIMESTAMP = 1 << 1; // Bit 1
    private static final int FLAG_TEMPERATURE_TYPE = 1 << 2; // Bit 2
    private static final int INVALID_TEMPERATURE = 0x007FFFFF;


    public List<List<String>> parseCoreBodyTemp(byte[] value){
        if (value == null || value.length < 1) {
            return new ArrayList<>();
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        List<List<String>> result = new ArrayList<>();

        int offset = 0;
        int flags = value[offset++] & 0xFF;

        boolean hasSkinTemperature = (flags & FLAG_SKIN_TEMPERATURE) != 0;
        boolean hasCoreReserved = (flags & FLAG_CORE_RESERVED) != 0;
        boolean hasQualityAndState = (flags & FLAG_QUALITY_AND_STATE) != 0;
        boolean isTemperatureInFahrenheit = (flags & FLAG_TEMPERATURE_UNIT) != 0;
        boolean hasHeartRate = (flags & FLAG_HEART_RATE) != 0;


        int coreBodyTemperature = readSInt16(value, offset);
        offset += 2;
        float coreBodyTemperatureFloat = coreBodyTemperature / 100.0f;

        List<String> coreBodyTempList = Arrays.asList("Core Body Temperature", String.valueOf(coreBodyTemperatureFloat), "Celsius", timestamp);
        result.add(coreBodyTempList);

        if (hasSkinTemperature) {
            int skinTemperature = readSInt16(value, offset);
            offset += 2;
            if (isTemperatureInFahrenheit) {
                skinTemperature = convertToCelsius(skinTemperature);
            }
            float skinTemperatureFloat = skinTemperature / 100.0f;
//            Log.i("parseBodyTemperatureData", "Skin Temperature: " + skinTemperatureFloat);
            List<String> skinTemperatureList = Arrays.asList("Skin Temperature", String.valueOf(skinTemperatureFloat), "Celsius", timestamp);
            result.add(skinTemperatureList);
        }

        if (hasCoreReserved) {
            int coreReserved = readSInt16(value, offset);
            offset += 2;
            float coreReservedFloat = coreReserved / 100.0f;
//            Log.i("parseBodyTemperatureData", "Core: " + coreReservedFloat);
        }

        if (hasQualityAndState) {
            int qualityAndState = readUInt8(value, offset);
            offset += 1;
//            Log.i("parseBodyTemperatureData", "Quality & State: " + qualityAndState);
        }

        if (hasHeartRate) {
            int heartRate = readUInt8(value, offset);
            offset += 1;
//            Log.i("parseBodyTemperatureData", "Quality & State: " + heartRate);
            List<String> heartRateList = Arrays.asList("Heart Rate", String.valueOf(heartRate), "bpm", timestamp);
            result.add(heartRateList);
        }

        return result;
    }

    public void parseTempMeasurement(byte[] value){
        if (value == null || value.length < 1) {
            return;
        }

        int flags = value[0] & 0xFF;

        boolean isTemperatureInFahrenheit = (flags & FLAG_TEMPERATURE_UNIT_2) != 0;
        boolean hasTimestamp = (flags & FLAG_TIMESTAMP) != 0;
        boolean hasTemperatureType = (flags & FLAG_TEMPERATURE_TYPE) != 0;

        int intValue = ((value[4] & 0xFF) << 24) |
                ((value[3] & 0xFF) << 16) |
                ((value[2] & 0xFF) << 8) |
                (value[1] & 0xFF);


        if (intValue == INVALID_TEMPERATURE) {
            Log.i("CoreParser", "Temperature value is NaN (Not a Number)");
        } else {
            int exponent = value[4];
            int mantissa = ((value[3] & 0xFF) << 16) | ((value[2] & 0xFF) << 8) | (value[1] & 0xFF);
            float temperatureFloat = (float) (mantissa * Math.pow(10, exponent));

            if(isTemperatureInFahrenheit){
                temperatureFloat = convertToCelsius((int)temperatureFloat);
            }
//            Log.i("CoreParser", "Temperature: " + temperatureFloat);


            if (hasTimestamp) {
                int year = readUInt16(value, 5);
                int month = readUInt8(value, 7);
                int day = readUInt8(value, 8);
                int hour = readUInt8(value, 9);
                int minute = readUInt8(value, 10);
                int second = readUInt8(value, 11);

                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.YEAR, year);
                cal.set(Calendar.MONTH, month - 1);
                cal.set(Calendar.DAY_OF_MONTH, day);
                cal.set(Calendar.HOUR_OF_DAY, hour);
                cal.set(Calendar.MINUTE, minute);
                cal.set(Calendar.SECOND, second);
                cal.set(Calendar.MILLISECOND, 0);

                long timestampMillis = cal.getTimeInMillis();
                Log.i("CoreParser", "Time: " + timestampMillis);

            }

            short temperatureType = 0;
            String bodyLocation;
            if(!hasTimestamp && hasTemperatureType){
                temperatureType= (short) readUInt8(value,5);
            }
            else if (hasTimestamp && hasTemperatureType){
                temperatureType = (short) readUInt8(value, 12);
            }

            switch (temperatureType){
                case 0x02:
                    bodyLocation = "Core"; break;
                default:
                    bodyLocation = "Unknown Location";
            }
//            Log.i("CoreParser", "Body Location: " + bodyLocation);
        }

    }

    public  Integer parseBatteryLevel(byte[] value){
        if (value == null || value.length < 1) {
            return null;
        }

        return readUInt8(value, 0);
    }

    private int convertToCelsius(int skinTemperature) {
        return (skinTemperature - 32) * 5 / 9;
    }

    private int readUInt8(byte[] value, int offset) {
        return value[offset] & 0xFF;
    }

    private int readUInt16(byte[] value, int offset) {
        return (value[offset] & 0xFF) | ((value[offset + 1] & 0xFF) << 8);
    }

    private int readSInt16(byte[] value, int offset) {
        return (value[offset] & 0xFF) | (value[offset + 1] << 8);
    }
}
