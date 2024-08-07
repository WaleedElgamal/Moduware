package com.example.moduware;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * Data adapter for updating live streamed-data in ConnectedDeviceActivity view.
 */
public class StreamedDataAdapter extends BaseAdapter{
    private LayoutInflater inflater;
    private List<List<String>> data;

    public StreamedDataAdapter(Context context, List<List<String>> data) {
        inflater = LayoutInflater.from(context);
        this.data=data;
    }

    public void updateData(List<List<String>> newData) {
        this.data.clear();
        this.data.addAll(newData);
        notifyDataSetChanged();
    }

    public void clear() {
        this.data.clear();
        notifyDataSetChanged();
    }

    public void addAll(List<List<String>> newData) {
        this.data.addAll(newData);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @SuppressLint("MissingPermission")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        com.example.moduware.StreamedDataAdapter.ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item_data, parent, false);
            holder = new com.example.moduware.StreamedDataAdapter.ViewHolder();
            holder.measurement = convertView.findViewById(R.id.measurement);
            holder.value = convertView.findViewById(R.id.value);
            holder.unit = convertView.findViewById(R.id.unit);
            holder.timestamp = convertView.findViewById(R.id.timestamp);
            convertView.setTag(holder);
        } else {
            holder = (com.example.moduware.StreamedDataAdapter.ViewHolder) convertView.getTag();
        }

        List<String> streamData = data.get(position);
        if (streamData != null) {
            if(streamData.size()>2){
                holder.measurement.setText(streamData.get(0));
                holder.value.setText(streamData.get(1));
                holder.unit.setText(streamData.get(2));
                holder.timestamp.setText(String.format("Latest received value at: %s", streamData.get(3)));
            }
        }
        return convertView;
    }



    private static class ViewHolder {
        TextView measurement;
        TextView value;
        TextView unit;
        TextView timestamp;
    }
}
