package plugins;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.example.moduware.DataHandler;
import com.example.moduware.MainActivity;
import com.example.moduware.WearableData;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

/**
 * The GattCallback class for the CORE sensor. Implemented functionality includes: discovering
 * services, subscribing to characteristics and handling characteristic changes.
 */

public class CoreGattCallback extends BaseGattCallback {

    /**
     * Services and Characteristics UUIDs
     **/
    private static final UUID BODY_TEMPERATURE_SERVICE = UUID.fromString("00002100-5B1E-4347-B07C-97B514DAE121");
    private static final UUID CORE_BODY_TEMPERATURE_CHAR = UUID.fromString("00002101-5B1E-4347-B07C-97B514DAE121");
    private static final UUID TEMPERATURE_CONTROL_POINT_CHAR = UUID.fromString("00002102-5B1E-4347-B07C-97B514DAE121");

    private static final UUID HEALTH_THERMOMETER_SERVICE = UUID.fromString("00001809-0000-1000-8000-00805F9B34FB");
    private static final UUID TEMPERATURE_MEASUREMENT_CHAR = UUID.fromString("00002A1C-0000-1000-8000-00805F9B34FB");
    private static final UUID TEMPERATURE_TYPE_CHAR = UUID.fromString("00002A1D-0000-1000-8000-00805F9B34FB");

    private static final UUID BATTERY_SERVICE = UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB");
    private static final UUID BATTERY_LEVEL_CHAR = UUID.fromString("00002A19-0000-1000-8000-00805F9B34FB");

    private static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");


    private BluetoothGattCharacteristic gattCharBodyTemp = null;
    private BluetoothGattCharacteristic gattCharTempControlPoint = null;
    private BluetoothGattCharacteristic gattCharTempMeasurement = null;
    private BluetoothGattCharacteristic gattCharTempType = null;
    private BluetoothGattCharacteristic gattCharBatteryLevel = null;

    private Queue<Runnable> commandQueue;
    private boolean commandQueueBusy;
    private Handler bleHandler = new Handler();

    private final CoreParser coreParser = new CoreParser();

    protected List<List<String>> streamedData;
    protected MutableLiveData<List<List<String>>> streamedLiveData;
    protected MutableLiveData<Integer> batteryLevelLiveData;
    private Handler handler = new Handler();
    private Runnable readBatteryLevelRunnable;
    private DataHandler dataHandler;

    public CoreGattCallback(){
        commandQueue = new LinkedList<>();
        commandQueueBusy = false;
        streamedData = new ArrayList<>();
        streamedLiveData = new MutableLiveData<>();
        batteryLevelLiveData = new MutableLiveData<>();
        streamedLiveData.setValue(streamedData);
        MainActivity mainActivity = MainActivity.getInstance();
        if (mainActivity != null) {
            dataHandler = mainActivity.getDataHandler();
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            Log.i("TAG", "Disconnected from GATT server.");
            gatt.close();
            stopBatteryLevelPolling();
        }
        else if (newState == BluetoothProfile.STATE_CONNECTING) {
            Log.i("TAG", "Connecting GATT server.");
        }
        else if (newState == BluetoothProfile.STATE_CONNECTED) {
            Log.i("TAG", "Connected to GATT server.");
            sensorName = gatt.getDevice().getName();
            sensorAddress = gatt.getDevice().getAddress();
            gatt.discoverServices();
        }
        else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
            Log.i("TAG", "Disconnecting from GATT server.");
        }
        connectionState.postValue(newState);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        List<BluetoothGattService> services = gatt.getServices();

        for(BluetoothGattService service: services){
            if(BODY_TEMPERATURE_SERVICE.equals(service.getUuid())){
                gattCharBodyTemp = service.getCharacteristic(CORE_BODY_TEMPERATURE_CHAR);
                if(gattCharBodyTemp!=null){
                    enableNotifications(gatt, gattCharBodyTemp);
                }
                gattCharTempControlPoint = service.getCharacteristic(TEMPERATURE_CONTROL_POINT_CHAR);
                if(gattCharTempControlPoint!=null){
                    enableIndications(gatt, gattCharTempControlPoint);
                }
            }
            else if(HEALTH_THERMOMETER_SERVICE.equals(service.getUuid())){
                gattCharTempMeasurement = service.getCharacteristic(TEMPERATURE_MEASUREMENT_CHAR);
                if (gattCharTempMeasurement != null) {
                    enableNotifications(gatt, gattCharTempMeasurement);
                }
                gattCharTempType = service.getCharacteristic(TEMPERATURE_TYPE_CHAR);
                if (gattCharTempType != null) {
                    enableNotifications(gatt, gattCharTempType);
                }
            }
            else if (BATTERY_SERVICE.equals(service.getUuid())){
                gattCharBatteryLevel = service.getCharacteristic(BATTERY_LEVEL_CHAR);
                if (gattCharBatteryLevel != null) {
                    startBatteryLevelPolling(gatt);
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void startBatteryLevelPolling(BluetoothGatt gatt) {
        readBatteryLevelRunnable = new Runnable() {
            @Override
            public void run() {
                if (gatt != null && gattCharBatteryLevel != null) {
                    gatt.readCharacteristic(gattCharBatteryLevel);
                    handler.postDelayed(this, 60000);
                }
            }
        };
        handler.post(readBatteryLevelRunnable);
    }

    private void stopBatteryLevelPolling() {
        handler.removeCallbacks(readBatteryLevelRunnable);
    }

    private void completedCommand() {
        commandQueueBusy = false;
        commandQueue.poll();
        nextCommand();
    }

    private void nextCommand() {
        if(commandQueueBusy) {
            return;
        }

        if (commandQueue.size() > 0) {
            final Runnable bluetoothCommand = commandQueue.peek();
            commandQueueBusy = true;

            bleHandler.post(() -> {
                try {
                    assert bluetoothCommand != null;
                    bluetoothCommand.run();
                } catch (Exception ex) {
                    Log.e("TAG", "ERROR: Command exception" + ex);
                }
            });
        }
    }

    @SuppressLint("MissingPermission")
    private void enableIndications(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        gatt.setCharacteristicNotification(characteristic, true);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CCCD);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            commandQueue.add(() -> gatt.writeDescriptor(descriptor));
            nextCommand();
        }
    }

    @SuppressLint("MissingPermission")
    private void enableNotifications(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        gatt.setCharacteristicNotification(characteristic, true);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CCCD);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            commandQueue.add(() -> gatt.writeDescriptor(descriptor));
            nextCommand();
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        completedCommand();
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        handleCharacteristicRead(characteristic, characteristic.getValue());
        completedCommand();
    }


    @SuppressLint("MissingPermission")
    @Override
    public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
        handleCharacteristicChanged(characteristic, value);
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

    }

    @Override
    public void handleCharacteristicRead(BluetoothGattCharacteristic characteristic, byte[] value) {
        UUID uuid = characteristic.getUuid();
        if (uuid.equals(BATTERY_LEVEL_CHAR)) {
            Integer data = coreParser.parseBatteryLevel(value);
            if(data!=null)
                batteryLevelLiveData.postValue(data);
        }
    }

    @Override
    public void handleCharacteristicChanged(BluetoothGattCharacteristic characteristic, byte[] value) {
        UUID uuid = characteristic.getUuid();
        final byte[] valueCopy = new byte[value.length];
        System.arraycopy(value, 0, valueCopy, 0, value.length );
        List<List<String>> data = new ArrayList<>();

        if (uuid.equals(CORE_BODY_TEMPERATURE_CHAR)) {
            data= coreParser.parseCoreBodyTemp(valueCopy);
            streamedLiveData.postValue(data);
        }
        else if (uuid.equals(TEMPERATURE_MEASUREMENT_CHAR)){
            coreParser.parseTempMeasurement(valueCopy);
        }
        else if (uuid.equals(BATTERY_LEVEL_CHAR)){
            Integer batteryLevel = coreParser.parseBatteryLevel(valueCopy);
            if(batteryLevel!=null)
                batteryLevelLiveData.postValue(batteryLevel);
        }

        for(List<String> dataList: data){
            WearableData dataObject = new WearableData(sensorAddress, sensorName.toString(), dataList);
            dataHandler.saveData(dataObject);
        }
    }

    public MutableLiveData<List<List<String>>> updateData(){
        return streamedLiveData;
    }

    public MutableLiveData<Integer> updateBatteryLevel(){
        return batteryLevelLiveData;
    }

    @Override
    public List<List<String>> getStreamedData() {
        return streamedData;
    }

}
