package com.example.moduware;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.widget.Toolbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import plugins.CorePluginService;
import plugins.PluginService;


public class ConnectedDeviceActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private Intent pluginIntent;
    private PluginService connectedPlugin;
    private TextView deviceName;
    private TextView deviceId;
    private TextView batteryLevel;
    private ConstraintLayout pluginButton;
    private Button disconnect;
    private boolean mBound;

    private StreamedDataAdapter dataAdapter;
    private ConstraintLayout listHeader;
    private ListView dataListView;

    private Button exportHistory;
    private Button exportStream;

    private DataHandler dataHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connected_device_activity);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.getNavigationIcon().setColorFilter(Color.parseColor("#ffffff"), PorterDuff.Mode.SRC_ATOP);

        initBackPress();

        Intent intent = getIntent();
        String pluginName = intent.getStringExtra("pluginName");

        connectedPlugin = getPluginService(pluginName);

        pluginIntent = new Intent(this,connectedPlugin.getClass());
        bindService(pluginIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        deviceName = findViewById(R.id.deviceName);
        deviceId = findViewById(R.id.deviceId);
        batteryLevel = findViewById(R.id.batteryLevel);
        pluginButton = findViewById(R.id.pluginButton);
        disconnect = findViewById(R.id.disconnectButton);
        disconnect.setOnClickListener(v -> {disconnect();});

        listHeader = findViewById(R.id.list_item_header);
        dataListView = findViewById(R.id.dataListView);

        exportHistory = findViewById(R.id.exportHistory);
        exportStream = findViewById(R.id.exportStream);

        MainActivity mainActivity = MainActivity.getInstance();
        if (mainActivity != null) {
            dataHandler = mainActivity.getDataHandler();
        }

        initView();
        displayDeviceInfo();
        initExportButtons();
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            if (className.equals(new ComponentName(ConnectedDeviceActivity.this, CorePluginService.class))) {
                CorePluginService.LocalBinder binder = (CorePluginService.LocalBinder) service;
                connectedPlugin = binder.getService();
                Log.i("TAG", "connects");
                mBound = true;
            }

            displayDeviceInfo();
            dataAdapter = new StreamedDataAdapter(ConnectedDeviceActivity.this, connectedPlugin.getGattCallback().getStreamedData());
            dataListView.setAdapter(dataAdapter);
            displayStreamedData();
            displayBatteryLevel();
            monitorConnectionState();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mBound = false;
            connectedPlugin=null;
        }
    };

    private PluginService getPluginService(String pluginName) {
        switch (pluginName){
            case "Core":
                return new CorePluginService();
            default:
                return null;
        }
    }

    @SuppressLint("DiscouragedApi")
    private void initView() {
        String pluginName = connectedPlugin.getName().toLowerCase();
        Log.i("TAG", pluginName);
        int imageResId = getResources().getIdentifier(pluginName +"_icon", "drawable", getPackageName());
        int textResId = getResources().getIdentifier(pluginName, "string", getPackageName());

        ImageButton pluginButtonIcon = pluginButton.findViewById(R.id.plugin_image_button);
        TextView pluginButtonName = pluginButton.findViewById(R.id.plugin_text);

        pluginButtonIcon.setImageResource(imageResId);
        pluginButtonIcon.setBackgroundResource(R.drawable.button_connected);
        pluginButtonName.setText(textResId);


        TextView measurement = listHeader.findViewById(R.id.measurement);
        TextView value = listHeader.findViewById(R.id.value);
        TextView unit = listHeader.findViewById(R.id.unit);

        measurement.setText("Measurement Type");
        measurement.setTypeface(measurement.getTypeface(), Typeface.BOLD);
        value.setText("Value");
        value.setTypeface(value.getTypeface(), Typeface.BOLD);
        unit.setText("Unit");
        unit.setTypeface(unit.getTypeface(), Typeface.BOLD);
    }

    private void displayDeviceInfo() {
        if(connectedPlugin!=null){
            if (connectedPlugin.getGattCallback()!=null){
                String sensorName = connectedPlugin.getGattCallback().getSensorName();
                deviceName.setText(sensorName != null && !sensorName.isEmpty() ? sensorName : "Unknown");
                deviceId.setText(connectedPlugin.getGattCallback().getSensorAddress());
            }
        }
    }

    private void displayBatteryLevel() {
        if(connectedPlugin!=null) {
            if(connectedPlugin.getGattCallback()!=null){
                connectedPlugin.getGattCallback().updateBatteryLevel().observe(ConnectedDeviceActivity.this, data -> {
                   batteryLevel.setText("Battery Level: " + data);
                });
            }
        }

    }

    private void displayStreamedData() {
        if(connectedPlugin!=null) {
            if(connectedPlugin.getGattCallback()!=null){
                connectedPlugin.getGattCallback().updateData().observe(ConnectedDeviceActivity.this, data -> {
                    dataAdapter.clear();
                    dataAdapter.addAll(data);
                    dataAdapter.notifyDataSetChanged();
                });
            }
        }
    }

    public void initBackPress() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(ConnectedDeviceActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initExportButtons() {
        exportHistory.setOnClickListener(v -> {
            //dataHandler.exportDataToFile("history_data_", deviceId.getText().toString());
        });
        exportStream.setOnClickListener(v -> {
            dataHandler.exportDataToFile("stream_data_", deviceId.getText().toString());
        });
    }

    private void monitorConnectionState() {
        connectedPlugin.getGattCallback().getConnectionState().observe(this, state -> {
            if(state == BluetoothProfile.STATE_DISCONNECTED)
               disconnect();
        });
    }
    private void disconnect() {
        if (connectedPlugin!= null) {
          connectedPlugin.disconnectDevice();
          stopService(new Intent(this, connectedPlugin.getClass()));
            Intent resultIntent = new Intent();
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        }
    }

}
