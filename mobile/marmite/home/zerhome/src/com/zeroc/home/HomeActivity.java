package com.zeroc.home;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.provider.AlarmClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ZerOS Home: a from-scratch Windows Phone style start screen.
 */
public class HomeActivity extends Activity {

    private static final String PREFS = "zerhome";
    private static final String KEY_TILES = "tiles";
    private static final String KEY_INITIALIZED = "initialized";

    private static final int COLUMNS = 4;
    private static final int ROWS = 6;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final List<AppEntry> mApps = new ArrayList<>();
    private final Map<String, AppEntry> mAppMap = new HashMap<>();

    private final LauncherApps.Callback mPackageCallback = new LauncherApps.Callback() {
        @Override
        public void onPackageAdded(String packageName, UserHandle user) {
            loadApps();
        }

        @Override
        public void onPackageRemoved(String packageName, UserHandle user) {
            loadApps();
        }

        @Override
        public void onPackageChanged(String packageName, UserHandle user) {
            loadApps();
        }

        @Override
        public void onPackagesAvailable(String[] packageNames, UserHandle user,
                boolean replacing) {
            loadApps();
        }

        @Override
        public void onPackagesUnavailable(String[] packageNames, UserHandle user,
                boolean replacing) {
            loadApps();
        }
    };

    private SharedPreferences mPrefs;
    private LauncherApps mLauncherApps;
    private TileGrid mGrid;
    private AppListPane mListPane;
    private TextView mHint;
    private int mStatusBarHeight;
    private int mNavHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.BLACK);
        applyImmersiveMode();

        mPrefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        mLauncherApps = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);
        mStatusBarHeight = statusBarHeight();
        mNavHeight = dp(48);

        buildUi();
        loadApps();
        handleIntent(getIntent());
    }

    /** Debug/automation hook: am start ... --ez open_apps true */
    private void handleIntent(Intent intent) {
        if (intent == null || mListPane == null) {
            return;
        }
        if (intent.getBooleanExtra("open_apps", false)) {
            mListPane.open(intent.getBooleanExtra("open_search", false));
        }
        String letter = intent.getStringExtra("letter");
        if (letter != null) {
            mListPane.jumpTo(letter);
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        FrameLayout content = new FrameLayout(this);
        content.setPadding(0, mStatusBarHeight, 0, 0);

        View scrim = new View(this);
        scrim.setBackgroundResource(R.drawable.start_scrim);
        content.addView(scrim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mGrid = new TileGrid(this);
        mGrid.setListener(new TileGrid.Listener() {
            @Override
            public void onLaunch(TileInfo info) {
                launch(info.component);
            }

            @Override
            public void onAppInfo(ComponentName component) {
                openAppInfo(component);
            }

            @Override
            public void onTilesChanged() {
                saveTiles();
                updateHint();
            }
        });
        FrameLayout.LayoutParams gridParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        gridParams.topMargin = dp(6);
        content.addView(mGrid, gridParams);

        mHint = new TextView(this);
        mHint.setText(R.string.empty_start);
        mHint.setTextColor(0x99FFFFFF);
        mHint.setTextSize(15);
        mHint.setGravity(Gravity.CENTER);
        mHint.setPadding(dp(40), 0, dp(40), 0);
        mHint.setVisibility(View.GONE);
        FrameLayout.LayoutParams hintParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintParams.topMargin = dp(120);
        content.addView(mHint, hintParams);

        mListPane = new AppListPane(this);
        mListPane.setListener(new AppListPane.Listener() {
            @Override
            public void onLaunch(AppEntry entry) {
                mListPane.close();
                launch(entry.component);
            }

            @Override
            public void onPin(AppEntry entry) {
                if (mGrid.addApp(entry)) {
                    Toast.makeText(HomeActivity.this, R.string.pinned, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(HomeActivity.this, R.string.no_space, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onAppInfo(AppEntry entry) {
                openAppInfo(entry.component);
            }
        });
        FrameLayout.LayoutParams listParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        content.addView(mListPane, listParams);

        root.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        NavBarView navBar = new NavBarView(this);
        navBar.setListener(new NavBarView.Listener() {
            @Override
            public void onBack() {
                handleBack();
            }

            @Override
            public void onHome() {
                mListPane.close();
            }

            @Override
            public void onSearch() {
                mListPane.open(true);
            }
        });
        root.addView(navBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, mNavHeight));

        setContentView(root);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mLauncherApps != null) {
            mLauncherApps.registerCallback(mPackageCallback);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mLauncherApps != null) {
            mLauncherApps.unregisterCallback(mPackageCallback);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mExecutor.shutdownNow();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (mListPane != null) {
            mListPane.close();
        }
        handleIntent(intent);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            applyImmersiveMode();
        }
    }

    @Override
    public void onBackPressed() {
        handleBack();
    }

    private void handleBack() {
        if (mListPane.isOpen()) {
            if (!mListPane.hideSearch()) {
                mListPane.close();
            }
            return;
        }
        if (mGrid.isDragging()) {
            mGrid.cancelDrag();
        }
        // Back on the Start screen itself does nothing, as on Windows Phone.
    }

    private void applyImmersiveMode() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private void loadApps() {
        final LauncherApps launcherApps = mLauncherApps;
        final int density = getResources().getDisplayMetrics().densityDpi;
        if (launcherApps == null) {
            return;
        }
        mExecutor.execute(() -> {
            List<AppEntry> entries = new ArrayList<>();
            List<LauncherActivityInfo> infos;
            try {
                infos = launcherApps.getActivityList(null, Process.myUserHandle());
            } catch (Exception e) {
                return;
            }
            for (LauncherActivityInfo info : infos) {
                try {
                    if (getPackageName().equals(info.getComponentName().getPackageName())) {
                        continue;
                    }
                    AppEntry entry = new AppEntry();
                    CharSequence label = info.getLabel();
                    if (label == null || label.length() == 0) {
                        label = info.getComponentName().getPackageName();
                    }
                    entry.label = label;
                    entry.component = info.getComponentName();
                    entry.icon = info.getBadgedIcon(density);
                    entry.letter = letterOf(label);
                    entries.add(entry);
                } catch (Exception ignored) {
                    // Skip packages with broken resources.
                }
            }
            final Collator collator = Collator.getInstance();
            Collections.sort(entries, (a, b) ->
                    collator.compare(a.label.toString(), b.label.toString()));
            runOnUiThread(() -> applyApps(entries));
        });
    }

    private void applyApps(List<AppEntry> entries) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        mApps.clear();
        mApps.addAll(entries);
        mAppMap.clear();
        for (AppEntry entry : mApps) {
            mAppMap.put(entry.key(), entry);
        }

        mListPane.setApps(mApps);
        mGrid.setApps(mAppMap);

        if (!mPrefs.getBoolean(KEY_INITIALIZED, false)) {
            createDefaultTiles();
            mPrefs.edit().putBoolean(KEY_INITIALIZED, true).apply();
            saveTiles();
        } else {
            List<TileInfo> tiles = readTiles();
            boolean removed = false;
            for (int i = tiles.size() - 1; i >= 0; i--) {
                if (!mAppMap.containsKey(tiles.get(i).key())) {
                    tiles.remove(i);
                    removed = true;
                }
            }
            mGrid.setTiles(tiles, mAppMap);
            if (removed) {
                saveTiles();
            }
        }
        updateHint();
    }

    private void createDefaultTiles() {
        List<TileInfo> tiles = new ArrayList<>();
        boolean[][] used = new boolean[ROWS][COLUMNS];

        addTile(tiles, used, resolveOrFind(new Intent(Intent.ACTION_DIAL), "dialer"), 0, 0, 2, 2);
        addTile(tiles, used, resolveOrFind(new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_APP_MESSAGING), "messaging", "sms", "mms"),
                2, 0, 2, 2);
        addTile(tiles, used, resolveOrFind(new Intent(Intent.ACTION_VIEW,
                Uri.parse("http://example.com")), "jelly", "browser", "chrome", "firefox"),
                0, 2, 4, 2);
        addTile(tiles, used, resolveOrFind(new Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                "snap", "camera"), 0, 4, 1, 1);
        addTile(tiles, used, resolveOrFind(new Intent(AlarmClock.ACTION_SHOW_ALARMS),
                "deskclock", "clock"), 1, 4, 1, 1);
        addTile(tiles, used, resolveOrFind(new Intent(Settings.ACTION_SETTINGS),
                "settings"), 2, 4, 1, 1);

        for (AppEntry app : mApps) {
            if (tiles.size() >= 10) {
                break;
            }
            if (hasPackage(tiles, app.component.getPackageName())) {
                continue;
            }
            int[] cell = findFreeCell(used, 1, 1);
            if (cell == null) {
                break;
            }
            used[cell[1]][cell[0]] = true;
            tiles.add(createTile(app, cell[0], cell[1], 1, 1));
        }

        mGrid.setTiles(tiles, mAppMap);
    }

    private void addTile(List<TileInfo> tiles, boolean[][] used, ComponentName component,
            int col, int row, int spanX, int spanY) {
        if (component == null || !fits(used, col, row, spanX, spanY)) {
            return;
        }
        AppEntry app = findApp(component);
        if (app == null || hasPackage(tiles, app.component.getPackageName())) {
            return;
        }
        for (int r = row; r < row + spanY; r++) {
            for (int c = col; c < col + spanX; c++) {
                used[r][c] = true;
            }
        }
        tiles.add(createTile(app, col, row, spanX, spanY));
    }

    private TileInfo createTile(AppEntry app, int col, int row, int spanX, int spanY) {
        TileInfo info = new TileInfo();
        info.component = app.component;
        info.col = col;
        info.row = row;
        info.spanX = spanX;
        info.spanY = spanY;
        info.colorIndex = Palette.forKey(app.key());
        return info;
    }

    private boolean fits(boolean[][] used, int col, int row, int spanX, int spanY) {
        if (col < 0 || row < 0 || col + spanX > COLUMNS || row + spanY > ROWS) {
            return false;
        }
        for (int r = row; r < row + spanY; r++) {
            for (int c = col; c < col + spanX; c++) {
                if (used[r][c]) {
                    return false;
                }
            }
        }
        return true;
    }

    private int[] findFreeCell(boolean[][] used, int spanX, int spanY) {
        for (int row = 0; row + spanY <= ROWS; row++) {
            for (int col = 0; col + spanX <= COLUMNS; col++) {
                if (fits(used, col, row, spanX, spanY)) {
                    return new int[]{col, row};
                }
            }
        }
        return null;
    }

    private boolean hasPackage(List<TileInfo> tiles, String packageName) {
        for (TileInfo info : tiles) {
            if (info.component.getPackageName().equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private AppEntry findApp(ComponentName component) {
        AppEntry exact = mAppMap.get(component.flattenToString());
        if (exact != null) {
            return exact;
        }
        for (AppEntry entry : mApps) {
            if (entry.component.getPackageName().equals(component.getPackageName())) {
                return entry;
            }
        }
        return null;
    }

    /** Prefer a real app over whoever stole the intent filter. */
    private ComponentName resolveOrFind(Intent intent, String... packageHints) {
        for (AppEntry app : mApps) {
            String pkg = app.component.getPackageName().toLowerCase(Locale.ROOT);
            for (String hint : packageHints) {
                if (pkg.contains(hint)) {
                    return app.component;
                }
            }
        }
        return resolve(intent);
    }

    private ComponentName resolve(Intent intent) {
        List<ResolveInfo> candidates = getPackageManager().queryIntentActivities(intent, 0);
        for (ResolveInfo info : candidates) {
            if (info.activityInfo == null) {
                continue;
            }
            String pkg = info.activityInfo.packageName;
            if (pkg.equals(getPackageName()) || isLauncherPackage(pkg)) {
                continue;
            }
            return new ComponentName(pkg, info.activityInfo.name);
        }
        return null;
    }

    /** Launchers often register DIAL / VIEW handlers for their own tiles. */
    private boolean isLauncherPackage(String pkg) {
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        for (ResolveInfo info : getPackageManager().queryIntentActivities(home, 0)) {
            if (info.activityInfo != null && pkg.equals(info.activityInfo.packageName)) {
                return true;
            }
        }
        return false;
    }

    private List<TileInfo> readTiles() {
        List<TileInfo> tiles = new ArrayList<>();
        String data = mPrefs.getString(KEY_TILES, "");
        if (data == null || data.isEmpty()) {
            return tiles;
        }
        for (String line : data.split("\n")) {
            String[] parts = line.split(",");
            if (parts.length != 6) {
                continue;
            }
            ComponentName component = ComponentName.unflattenFromString(parts[0]);
            if (component == null) {
                continue;
            }
            try {
                TileInfo info = new TileInfo();
                info.component = component;
                info.col = Integer.parseInt(parts[1]);
                info.row = Integer.parseInt(parts[2]);
                info.spanX = Integer.parseInt(parts[3]);
                info.spanY = Integer.parseInt(parts[4]);
                info.colorIndex = Integer.parseInt(parts[5]);
                tiles.add(info);
            } catch (NumberFormatException ignored) {
                // Skip broken lines.
            }
        }
        return tiles;
    }

    private void saveTiles() {
        StringBuilder builder = new StringBuilder();
        for (TileInfo info : mGrid.getTiles()) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(info.key()).append(',')
                    .append(info.col).append(',')
                    .append(info.row).append(',')
                    .append(info.spanX).append(',')
                    .append(info.spanY).append(',')
                    .append(info.colorIndex);
        }
        mPrefs.edit().putString(KEY_TILES, builder.toString()).apply();
    }

    private void updateHint() {
        if (mHint != null) {
            mHint.setVisibility(mGrid.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void launch(ComponentName component) {
        if (component == null) {
            return;
        }
        try {
            ActivityOptions options = ActivityOptions.makeCustomAnimation(this,
                    android.R.anim.fade_in, android.R.anim.fade_out);
            mLauncherApps.startMainActivity(component, Process.myUserHandle(), null,
                    options.toBundle());
        } catch (Exception e) {
            try {
                startActivity(new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_LAUNCHER)
                        .setComponent(component)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
            } catch (Exception ignored) {
                Toast.makeText(this, R.string.launch_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openAppInfo(ComponentName component) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", component.getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (Exception ignored) {
            Toast.makeText(this, R.string.launch_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private static String letterOf(CharSequence label) {
        if (label == null || label.length() == 0) {
            return "#";
        }
        char first = label.charAt(0);
        if (!Character.isLetter(first)) {
            return "#";
        }
        return new String(Character.toChars(first)).toUpperCase(Locale.getDefault());
    }

    private int statusBarHeight() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (id > 0) {
            return getResources().getDimensionPixelSize(id);
        }
        return dp(24);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
