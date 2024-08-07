package com.example.moduware;

import android.app.Activity;

import plugins.CorePluginService;
import plugins.PluginService;

import java.util.ArrayList;
import java.util.List;

/**
 * Any new sensor plugin should be added to the list of plugins here
 */

public class PluginManager {
    private Activity activity;
    private List<PluginService> plugins;
    public PluginManager(Activity activity){
        this.activity = activity;
        plugins = new ArrayList<>();
        plugins.add(new CorePluginService());
    }

    public List<PluginService> getPlugins() {
        return plugins;
    }
}
