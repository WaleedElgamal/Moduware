package plugins;

import android.bluetooth.BluetoothDevice;

import androidx.lifecycle.LiveData;

import java.util.List;

/**
 * Interface that should be implemented by a WearablePluginService class.
 **/

public interface PluginService {
    LiveData<List<BluetoothDevice>> scanDevices();
    List<BluetoothDevice> getDevicesList();
    BaseGattCallback getGattCallback();
    void stopScan();
    void connectDevice(BluetoothDevice device);
    boolean isConnected();
    void disconnectDevice();
    String getName();
}
