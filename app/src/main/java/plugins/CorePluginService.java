package plugins;

import static android.bluetooth.le.ScanSettings.SCAN_MODE_BALANCED;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.moduware.MainActivity;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * The PluginService class for the CORE sensor. Implemented functionality includes: scanning for
 * CORE sensors, connecting and disconnecting.
 */

public class CorePluginService extends Service implements PluginService {
    private String name = "Core";
    protected BluetoothManager bluetoothManager;
    protected BluetoothAdapter bluetoothAdapter;
    protected BluetoothLeScanner bluetoothLeScanner;
    protected BluetoothGatt bluetoothGatt;
    protected LinkedList<BluetoothGatt> gattInstances;
    private CoreGattCallback gattCallback;
    protected List<BluetoothDevice> currentDevices;
    protected MutableLiveData<List<BluetoothDevice>> scannedDevicesLiveData;

    /**
     * Sensors that implement the CoreTemp Service will advertise
     * the UUID of the CoreTemp Service in their advertisement data.
     */
    protected UUID serviceUUIDOld = UUID.fromString("00004200-F366-40B2-AC37-70CCE0AA83B1");
    protected UUID serviceUUIDNew = UUID.fromString("00002100-5B1E-4347-B07C-97B514DAE121");

    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "SensorServiceChannel";

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public CorePluginService getService() {
            return CorePluginService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        currentDevices = new ArrayList<>();
        scannedDevicesLiveData = new MutableLiveData<>();
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        gattCallback = new CoreGattCallback();
        gattInstances = new LinkedList<>();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, getNotification());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Sensor Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification getNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Sensor Service")
                .setContentText("Collecting data from sensor")
                .setContentIntent(pendingIntent)
                .build();
    }

    @SuppressLint("MissingPermission")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @SuppressLint("MissingPermission")
    @Override
    public LiveData<List<BluetoothDevice>> scanDevices() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_SHORT).show();
            return scannedDevicesLiveData;
        }

        if(currentDevices.size()>0){
            currentDevices.clear();
        }

        ScanFilter scanFilter1 = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(serviceUUIDOld))
                .build();

        ScanFilter scanFilter2 = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(serviceUUIDNew))
                .build();

        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(SCAN_MODE_BALANCED)
                .build();

        List<ScanFilter> filters = new ArrayList<>();
        filters.add(scanFilter1);
        filters.add(scanFilter2);

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        bluetoothLeScanner.startScan(filters, scanSettings, leScanCallback);

        new Handler().postDelayed(() -> bluetoothLeScanner.stopScan(leScanCallback), 5000);

        return scannedDevicesLiveData;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void stopScan() {
        bluetoothLeScanner.stopScan(leScanCallback);
    }

    public final ScanCallback leScanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            currentDevices = scannedDevicesLiveData.getValue();
            if (currentDevices == null) {
                currentDevices = new ArrayList<>();
            }
            if (!currentDevices.contains(device)) {
                currentDevices.add(device);
                scannedDevicesLiveData.postValue(currentDevices);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Core Plugin", "Scan failed with error: " + errorCode);
        }
    };

    public List<BluetoothDevice> getDevicesList() {
        return currentDevices;
    }


    public BaseGattCallback getGattCallback() {
        return gattCallback;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void connectDevice(BluetoothDevice device) {
        if (bluetoothGatt==null) {
            bluetoothGatt = device.connectGatt(this, false, gattCallback);
            gattInstances.add(bluetoothGatt);
        }
    }

    @Override
    public boolean isConnected() {
        if(gattCallback!=null){
            if(gattCallback.connectionState.isInitialized()  && gattCallback.connectionState.getValue()== BluetoothProfile.STATE_CONNECTED){
                currentDevices.clear();;
                currentDevices.add(bluetoothGatt.getDevice());
                return true;
            }
        }
        return false;
    }

    @SuppressLint("MissingPermission")
    public void disconnectDevice(){
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt=null;
        }
        while(!gattInstances.isEmpty()) {
           BluetoothGatt gatt =  gattInstances.pop();
           gatt.disconnect();
           gatt = null;
        }
    }

    @NonNull
    public String getName(){
        return name;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnectDevice();
    }
}
