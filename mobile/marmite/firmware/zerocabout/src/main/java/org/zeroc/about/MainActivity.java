package org.zeroc.about;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ZerOS "About phone" screen.
 *
 * Material Design 2: MaterialToolbar + TabLayout in an AppBarLayout,
 * MaterialCardView cards and a contained MaterialButton. Everything is read
 * from Build, system properties (getprop), ActivityManager and StatFs.
 */
public class MainActivity extends AppCompatActivity {

    private final View[] pages = new View[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setSubtitle("ZerOS " + roman() + " · " + version());

        fill(R.id.system_rows, new String[][]{
                {getString(R.string.label_android), androidVersion()},
                {getString(R.string.label_security), securityPatch()},
                {getString(R.string.label_kernel), kernel()},
                {getString(R.string.label_build), buildId()},
                {getString(R.string.label_build_date), buildDate()},
        });
        fill(R.id.device_rows, new String[][]{
                {getString(R.string.label_model), model()},
                {getString(R.string.label_device), Build.DEVICE},
                {getString(R.string.label_platform), platform()},
                {getString(R.string.label_screen), screen()},
                {getString(R.string.label_memory), memory()},
                {getString(R.string.label_storage), storage()},
        });
        fill(R.id.zeros_rows, new String[][]{
                {getString(R.string.label_generation), generation()},
                {getString(R.string.label_version), version()},
                {getString(R.string.label_release), releaseType()},
                {getString(R.string.label_sdk), sdk()},
        });

        pages[0] = findViewById(R.id.page_system);
        pages[1] = findViewById(R.id.page_device);
        pages[2] = findViewById(R.id.page_zeros);

        MaterialButton updates = findViewById(R.id.button_updates);
        updates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUpdater();
            }
        });

        TabLayout tabs = findViewById(R.id.tabs);
        tabs.addTab(tabs.newTab().setText(R.string.section_system));
        tabs.addTab(tabs.newTab().setText(R.string.section_device));
        tabs.addTab(tabs.newTab().setText(R.string.section_zeros));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                show(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        int start = getIntent() == null ? 0 : getIntent().getIntExtra("tab", 0);
        start = Math.max(0, Math.min(pages.length - 1, start));
        TabLayout.Tab tab = tabs.getTabAt(start);
        if (tab != null) {
            tab.select();
        }
        show(start);
    }

    private void show(int index) {
        for (int i = 0; i < pages.length; i++) {
            pages[i].setVisibility(i == index ? View.VISIBLE : View.GONE);
        }
    }

    private void fill(int containerId, String[][] rows) {
        LinearLayout container = findViewById(containerId);
        LayoutInflater inflater = LayoutInflater.from(this);
        container.removeAllViews();
        int dividerHeight = Math.max(1, Math.round(getResources().getDisplayMetrics().density));
        for (int i = 0; i < rows.length; i++) {
            View row = inflater.inflate(R.layout.row_item, container, false);
            ((TextView) row.findViewById(R.id.row_label)).setText(rows[i][0]);
            ((TextView) row.findViewById(R.id.row_value)).setText(rows[i][1]);
            container.addView(row);
            if (i < rows.length - 1) {
                View divider = new View(this);
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dividerHeight));
                divider.setBackgroundColor(ContextCompat.getColor(this, R.color.zer_divider));
                container.addView(divider);
            }
        }
    }

    private void openUpdater() {
        try {
            Intent i = new Intent();
            i.setClassName("org.lineageos.updater",
                    "org.lineageos.updater.UpdatesActivity");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception ignored) {
            // Updater is not installed - nothing to open.
        }
    }

    // -------------------------------------------------------------------- data

    private String androidVersion() {
        return Build.VERSION.RELEASE + " · API " + Build.VERSION.SDK_INT;
    }

    private String securityPatch() {
        String raw = Build.VERSION.SECURITY_PATCH;
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(raw);
            return new SimpleDateFormat("d MMMM yyyy", new Locale("ru", "RU")).format(d);
        } catch (Exception e) {
            return raw;
        }
    }

    private String kernel() {
        return System.getProperty("os.version", "—");
    }

    private String buildId() {
        return prop("ro.build.display.id", Build.DISPLAY);
    }

    private String buildDate() {
        try {
            long utc = Long.parseLong(prop("ro.build.date.utc", "0"));
            if (utc > 0) {
                return new SimpleDateFormat("d MMMM yyyy", new Locale("ru", "RU"))
                        .format(new Date(utc * 1000L));
            }
        } catch (Exception ignored) {
            // fall through to the raw property
        }
        String raw = prop("ro.build.date", "");
        return raw.isEmpty() ? "—" : raw;
    }

    private String model() {
        return Build.MANUFACTURER + " " + Build.MODEL;
    }

    private String platform() {
        String board = prop("ro.board.platform", Build.HARDWARE);
        int cores = Runtime.getRuntime().availableProcessors();
        String soc;
        if ("msm8937".equals(board)) {
            soc = "Snapdragon 430";
        } else if ("msm8953".equals(board)) {
            soc = "Snapdragon 625";
        } else if ("msm8998".equals(board)) {
            soc = "Snapdragon 835";
        } else {
            soc = board;
        }
        return soc + " · " + cores + " " + coresWord(cores);
    }

    private String coresWord(int cores) {
        int mod10 = cores % 10;
        int mod100 = cores % 100;
        if (mod10 == 1 && mod100 != 11) {
            return "ядро";
        }
        if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) {
            return "ядра";
        }
        return "ядер";
    }

    private String screen() {
        DisplayMetrics m = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(m);
        return m.widthPixels + " × " + m.heightPixels + " · " + m.densityDpi + " dpi";
    }

    private String memory() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(info);
        return String.format(new Locale("ru", "RU"), "%.1f ГБ", info.totalMem / 1073741824.0);
    }

    private String storage() {
        try {
            StatFs fs = new StatFs(Environment.getDataDirectory().getPath());
            double total = fs.getTotalBytes() / 1073741824.0;
            double free = fs.getAvailableBytes() / 1073741824.0;
            return String.format(new Locale("ru", "RU"), "%.1f из %.0f ГБ свободно", free, total);
        } catch (Exception e) {
            return "—";
        }
    }

    private String generation() {
        return "ZerOS " + roman() + " · Android " + Build.VERSION.RELEASE;
    }

    private String roman() {
        String[] roman = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII"};
        int index = Build.VERSION.SDK_INT - 29;
        if (index >= 0 && index < roman.length) {
            return roman[index];
        }
        return "?";
    }

    private String version() {
        return prop("ro.lineage.version", "—");
    }

    private String sdk() {
        return prop("ro.lineage.build.version.plat.sdk", "—");
    }

    private String releaseType() {
        String type = prop("ro.lineage.releasetype", "");
        if ("OFFICIAL".equalsIgnoreCase(type)) {
            return getString(R.string.official);
        }
        if ("UNOFFICIAL".equalsIgnoreCase(type)) {
            return getString(R.string.unofficial);
        }
        return type.isEmpty() ? "—" : type;
    }

    /** Read a system property without touching hidden APIs. */
    private String prop(String key, String fallback) {
        BufferedReader reader = null;
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"getprop", key});
            reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            p.waitFor();
            if (line != null && !line.trim().isEmpty()) {
                return line.trim();
            }
        } catch (Exception ignored) {
            // fall through
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
        }
        return fallback;
    }
}
