package com.zeroc.home;

import android.content.ComponentName;

/** Position and look of a tile on the Start screen. */
public class TileInfo {
    public ComponentName component;
    public int col;
    public int row;
    public int spanX = 1;
    public int spanY = 1;
    public int colorIndex;

    public String key() {
        return component.flattenToString();
    }
}
