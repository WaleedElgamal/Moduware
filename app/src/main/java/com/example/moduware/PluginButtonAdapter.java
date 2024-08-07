package com.example.moduware;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/***
 * Button adapter for the list of plugins in the MainActivity view
 */
public class PluginButtonAdapter extends RecyclerView.Adapter<PluginButtonAdapter.ButtonViewHolder> {

    private List<PluginButton> pluginButtons;
    private OnItemClickListener listener;
    private int selectedItem = RecyclerView.NO_POSITION;


    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public static class ButtonViewHolder extends RecyclerView.ViewHolder {
        public ImageButton imageButton;
        public TextView textView;

        public ButtonViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
            super(itemView);
            imageButton = itemView.findViewById(R.id.plugin_image_button);
            textView = itemView.findViewById(R.id.plugin_text);

            imageButton.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(position);
                    }
                }
            });
        }

        public void bind(PluginButton pluginButton, boolean isSelected) {
            imageButton.setImageResource(pluginButton.getImageResId());
            textView.setText(pluginButton.getTextResId());

            // Set background based on selection
            if (isSelected) {
                imageButton.setBackgroundResource(R.drawable.button_pressed);
            } else {
                imageButton.setBackgroundResource(R.drawable.circular_button_background);
            }
        }
    }

    public PluginButtonAdapter(List<PluginButton> pluginButtons) {
        this.pluginButtons = pluginButtons;
    }

    @NonNull
    @Override
    public ButtonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.plugin_button, parent, false);
        return new ButtonViewHolder(itemView, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ButtonViewHolder holder, int position) {
        PluginButton pluginButton = pluginButtons.get(position);
        boolean isSelected = (position == selectedItem);
        holder.bind(pluginButton, isSelected);
    }

    @Override
    public int getItemCount() {
        return pluginButtons.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setSelectedItem(int position) {
        selectedItem = position;
        notifyDataSetChanged();
    }
}
