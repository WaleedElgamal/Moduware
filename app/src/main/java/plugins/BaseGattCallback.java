package plugins;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;

import androidx.lifecycle.MutableLiveData;

import java.util.List;

/**
 * Base class that a WearableGattCallback class should extend.
 **/

public abstract class BaseGattCallback extends BluetoothGattCallback{
    private static final String TAG = "BaseGattCallback";
    protected String sensorName;
    protected String sensorAddress;
    protected MutableLiveData<Integer> connectionState = new MutableLiveData<>();

    @Override
    public abstract void onConnectionStateChange(BluetoothGatt gatt, int status, int newState);

    @Override
    public abstract void onServicesDiscovered(BluetoothGatt gatt, int status);

    @Override
    public abstract void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status);

    @Override
    public abstract void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status);

    public abstract void handleCharacteristicRead(BluetoothGattCharacteristic characteristic, byte[] value);

    public abstract void handleCharacteristicChanged(BluetoothGattCharacteristic characteristic, byte[] value);

    public abstract MutableLiveData<List<List<String>>> updateData();

    public abstract MutableLiveData<Integer> updateBatteryLevel();

    public abstract List<List<String>> getStreamedData();

    public MutableLiveData<Integer> getConnectionState() {
        return connectionState;
    }

    public String getSensorName() {
        return sensorName;
    }

    public String getSensorAddress() {
        return sensorAddress;
    }
}
