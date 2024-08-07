package com.example.moduware;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * Device adapter for list of scanned bluetooth devices
 */
public class BluetoothDeviceAdapter extends BaseAdapter {

    private LayoutInflater inflater;
    private List<BluetoothDevice> devices;
    private OnDeviceClickListener onDeviceClickListener;
    private BluetoothDevice connectedDevice;


    public interface OnDeviceClickListener {
        void onDeviceClick(BluetoothDevice device);
    }
    public BluetoothDeviceAdapter(Context context, List<BluetoothDevice> devices, OnDeviceClickListener listener) {
        inflater = LayoutInflater.from(context);
        connectedDevice = null;
        this.devices=devices;
        this.onDeviceClickListener = listener;
    }

    public void updateDevices(List<BluetoothDevice> newDevices) {
        connectedDevice = null;
        this.devices.clear();
        this.devices.addAll(newDevices);
        notifyDataSetChanged();
    }

    public void clear() {
        connectedDevice = null;
        this.devices.clear();
        notifyDataSetChanged();
    }

    public void addAll(List<BluetoothDevice> newDevices) {
        this.devices.addAll(newDevices);
        notifyDataSetChanged();
    }

    public void setConnectedDevice(BluetoothDevice device) {
        this.connectedDevice = device;
    }

    @Override
    public int getCount() {
        return devices.size();
    }

    @Override
    public Object getItem(int position) {
        return devices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @SuppressLint("MissingPermission")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item_device, parent, false);
            holder = new ViewHolder();
            holder.deviceName = convertView.findViewById(R.id.deviceName);
            holder.deviceAddress = convertView.findViewById(R.id.deviceAddress);
            holder.connectionStatus = convertView.findViewById(R.id.connectionStatus);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        BluetoothDevice device = devices.get(position);
        if (device != null) {
            if(device.getName()!=null){
                holder.deviceName.setText(device.getName());
            }
            else{
                holder.deviceName.setText("Unknown");
            }
            holder.deviceAddress.setText(device.getAddress());

            if(device.equals(connectedDevice)){
                convertView.setBackgroundColor(Color.parseColor("#00DF61"));
                holder.connectionStatus.setText("Connected");
            }
            else{
                convertView.setBackgroundColor(Color.TRANSPARENT); // Reset to default
                holder.connectionStatus.setText("");
            }
        }

        convertView.setOnClickListener(v -> {
            if (onDeviceClickListener != null) {
                onDeviceClickListener.onDeviceClick(device);
            }
        });

        return convertView;
    }



    private static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView connectionStatus;
    }
}
