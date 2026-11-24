package com.zeroc.home;

/** Flat Windows Phone style tile colours. */
public final class Palette {

    private static final int[] TILES = {
            0xFF0078D7,
            0xFF1F7A6B,
            0xFFC0504D,
            0xFFE36C09,
            0xFF7A3B8F,
            0xFF2D7D46,
            0xFFA0522D,
            0xFF4A5568
    };

    private Palette() {
    }

    public static int count() {
        return TILES.length;
    }

    public static int color(int index) {
        return TILES[Math.floorMod(index, TILES.length)];
    }

    public static int forKey(String key) {
        return Math.floorMod(key.hashCode(), TILES.length);
    }
}
