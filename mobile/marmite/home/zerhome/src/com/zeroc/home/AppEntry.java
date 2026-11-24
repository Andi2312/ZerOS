package com.zeroc.home;

import android.content.ComponentName;
import android.graphics.drawable.Drawable;

/** One launchable activity as shown in the app list and on Start. */
public class AppEntry {
    public CharSequence label;
    public ComponentName component;
    public Drawable icon;
    public String letter;

    public String key() {
        return component.flattenToString();
    }
}
