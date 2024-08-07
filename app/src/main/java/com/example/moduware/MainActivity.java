package com.example.moduware;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.Toast;

import plugins.CorePluginService;
import plugins.PluginService;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    /**
    * Responsible for the top recycler view displaying supported plugins
     */
    private RecyclerView recyclerView;
    private PluginButtonAdapter adapter;
    private List<PluginButton> pluginButtons;
    private List<PluginService> supportedPlugins;
    private PluginManager pluginManager;
    private Intent pluginIntent;
    private PluginService mBoundService;
    private boolean mBound;


    private PluginService selectedPlugin;
    private TextView preScanText;
    private Button scanButton;
    private boolean isScanning;


    /**
     * Responsible for the bottom list view, displaying scanned bluetooth devices
     * specific to the chosen plugin. Uses LiveData-Observer pattern which updates
     * the list as soon as the devices are scanned
     */
    private BluetoothDeviceAdapter deviceAdapter;
    private ListView deviceListView;


    /**
     * Responsible for having a database instance
     */
    private DataHandler dataHandler;

    private static MainActivity instance;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_LOCATION_PERMISSION = 2;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 3;
    private boolean permissionsGranted = false;
    private AlertDialog connectionPopup;

    private ActivityResultLauncher<Intent> startForResult;


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        startForResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        initRecyclerView();
                        if(deviceAdapter!=null){
                            deviceAdapter.clear();
                            deviceAdapter.notifyDataSetChanged();
                        }
                        preScan();
                        scanButton.setVisibility(View.GONE);
                    } else {
                        Log.d("MainActivity", "Canceled or other result received from ConnectedDeviceActivity");
                    }
                }
        );

        pluginManager = new PluginManager(this);

        initPluginButtons();

        initRecyclerView();

        preScanText = findViewById(R.id.pre_scan_text);
        preScan();

        scanButton = findViewById(R.id.scanButton);
        isScanning = false;
        scan();

        deviceListView = findViewById(R.id.deviceListView);
        deviceAdapter =null;

        mBound = false;
        mBoundService = null;

        dataHandler = new DataHandler(this);

        instance = this;
    }


    @SuppressLint("DiscouragedApi")
    private void initPluginButtons() {
        pluginButtons = new ArrayList<>();
        supportedPlugins = pluginManager.getPlugins();
        Log.i("TAG", "initPluginButtons: " + supportedPlugins);

        for (PluginService plugin: supportedPlugins){
            String name = plugin.getName().toLowerCase();
            int imageResId = getResources().getIdentifier(name +"_icon", "drawable", getPackageName());
            int textResId = getResources().getIdentifier(name, "string", getPackageName());
            pluginButtons.add(new PluginButton(imageResId,textResId));
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void initRecyclerView() {
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new PluginButtonAdapter(pluginButtons);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(position -> {
            if(!isScanning){
                adapter.setSelectedItem(position);

                selectedPlugin = supportedPlugins.get(position);

                if(mBoundService!=null) {
                    unbindService(serviceConnection);
                }

                pluginIntent = new Intent(this,selectedPlugin.getClass());
                if(!isMyServiceRunning(selectedPlugin.getClass())) {
                    startForegroundService(pluginIntent);
                }
                bindService(pluginIntent, serviceConnection, Context.BIND_AUTO_CREATE);

            }
        });
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @SuppressLint("MissingPermission")
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            if (className.equals(new ComponentName(MainActivity.this, CorePluginService.class))) {
                CorePluginService.LocalBinder binder = (CorePluginService.LocalBinder) service;
                mBoundService = binder.getService();
                mBound = true;
                Log.i("TAG", "onServiceConnected: core");
            }

            if(deviceAdapter==null){
                deviceAdapter = new BluetoothDeviceAdapter(MainActivity.this, mBoundService.getDevicesList(), MainActivity.this::connect);
                deviceListView.setAdapter(deviceAdapter);
            }
            else{
                deviceAdapter.updateDevices(mBoundService.getDevicesList());
            }

            if(mBoundService.isConnected()){
                scanButton.setVisibility(View.GONE);
                preScanText.setText("");
                deviceAdapter.setConnectedDevice(mBoundService.getDevicesList().get(0));
                Log.i("TAG", "onServiceConnected: " + mBoundService.getDevicesList().get(0).getName());
            }
            else{
                scanButton.setVisibility(View.VISIBLE);
                if(mBoundService.getDevicesList().size()==0)
                    preScan();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mBound = false;
            mBoundService=null;
        }
    };

    private void preScan() {
        if(supportedPlugins.size()==0){
            preScanText.setText(R.string.no_supported_plugins);
        }
        else{
            preScanText.setText(R.string.select_plugin);
        }
    }

    //TODO add check if bluetooth is turned off before requesting permissions
    @SuppressLint("MissingPermission")
    private void scan() {
        scanButton.setOnClickListener(v -> {
            requestPermissions();
            if(permissionsGranted){
                isScanning = true;
                preScanText.setText("");
                scanButton.setText(R.string.scanning);
                scanButton.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.light_blue));
                scanButton.setEnabled(false);
                new Handler().postDelayed(() -> {
                    scanButton.setText(R.string.scan);
                    scanButton.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.blue_primary));
                    isScanning = false;
                    scanButton.setEnabled(true);
                }, 5000);
                mBoundService.scanDevices().observe(MainActivity.this, devices -> {
                    deviceAdapter.clear();
                    deviceAdapter.addAll(devices);
                    deviceAdapter.notifyDataSetChanged();
                });
            }
        });
    }

    private void connect(BluetoothDevice selectedDevice){
        if (selectedDevice != null) {
            mBoundService.stopScan();
            if(!mBoundService.isConnected()) {
                mBoundService.connectDevice(selectedDevice);
            }
            else{
                startActivity();
                return;
            }

            showConnectingPopup();
            mBoundService.getGattCallback().getConnectionState().observe(this, state -> {
                switch (state) {
                    case BluetoothProfile.STATE_DISCONNECTED:
                        if(connectionPopup!=null){
                            connectionPopup.setTitle("Failed to connect");
                            connectionPopup.setMessage("Could not connect to device");
                            new Handler().postDelayed(() -> {
                               connectionPopup.dismiss();
                            }, 2000);
                        }
                        break;
                    case BluetoothProfile.STATE_CONNECTED:
                        if(connectionPopup!=null)
                        {
                            connectionPopup.setTitle("Connected");
                            connectionPopup.setMessage("Device has connected successfully");
                            new Handler().postDelayed(() -> {
                                connectionPopup.dismiss();
                            }, 2000);
                            startActivity();
                        }
                        break;
                }
            });
        }
    }

    private void startActivity() {
        Intent intent = new Intent(MainActivity.this, ConnectedDeviceActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("pluginName",selectedPlugin.getName());
        startForResult.launch(intent);
    }

    private void stopAllServices() {
        for (PluginService pluginService : supportedPlugins) {
            stopService(new Intent(this, pluginService.getClass()));
        }
    }

    private void showConnectingPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Connecting...");
        builder.setMessage("Please wait..");
        builder.setCancelable(false);
        connectionPopup = builder.create();
        connectionPopup.show();
    }

    private void requestPermissions() {
        permissionsGranted=false;
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        List<String> permissions = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_ADVERTISE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            }
        }

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissions.toArray(new String[0]),
                    REQUEST_BLUETOOTH_PERMISSIONS);
        }
        else{
            permissionsGranted=true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(this, "Bluetooth is required to scan for devices", Toast.LENGTH_SHORT).show();
            permissionsGranted=false;
        }
        else{
            permissionsGranted=true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION && grantResults.length > 0
                && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission is required to scan for Bluetooth devices", Toast.LENGTH_SHORT).show();
            permissionsGranted=false;
            return;
        }

        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Bluetooth permissions are required. Grant them from the settings app.", Toast.LENGTH_SHORT).show();
                    permissionsGranted=false;
                    return;
                }
            }
        }
        permissionsGranted=true;
    }

    public DataHandler getDataHandler() {
        return dataHandler;
    }

    public static MainActivity getInstance(){
        return instance;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mBoundService!=null && mBoundService.isConnected()){
            deviceAdapter.clear();
            deviceAdapter.addAll(mBoundService.getDevicesList());
            deviceAdapter.setConnectedDevice(mBoundService.getDevicesList().get(0));
            deviceAdapter.notifyDataSetChanged();
            scanButton.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBound) {
            unbindService(serviceConnection);
            mBound = false;
            mBoundService = null;
        }
        stopAllServices();
        if(deviceAdapter!=null){
            deviceAdapter.clear();
            deviceAdapter.notifyDataSetChanged();
        }
        scanButton.setVisibility(View.GONE);
    }
}
