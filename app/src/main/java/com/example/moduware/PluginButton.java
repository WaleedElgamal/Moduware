package com.example.moduware;

public class PluginButton {
    private int imageResId;
    private int textResId;

    public PluginButton(int imageResId, int textResId) {
        this.imageResId = imageResId;
        this.textResId = textResId;
    }

    public int getImageResId() {
        return imageResId;
    }

    public int getTextResId() {
        return textResId;
    }
}
