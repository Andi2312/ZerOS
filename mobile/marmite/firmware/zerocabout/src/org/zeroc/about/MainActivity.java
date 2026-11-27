package org.zeroc.about;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ZerOS "About phone" screen.
 *
 * Material Design 2: top app bar, three tabs (System, Device, ZerOS) and
 * cards. Everything is read from Build, system properties (getprop),
 * ActivityManager and StatFs - no hardcoded device data.
 */
public class MainActivity extends Activity {

    private static final int BG = 0xFF14171A;
    private static final int CARD = 0xFF1E2427;
    private static final int ACCENT = 0xFFE6125E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFF9AA0A6;
    private static final int DIVIDER = 0xFF2A3033;
    private static final int FOOTER = 0xFF6E7478;

    private Typeface regular;
    private Typeface medium;
    private Typeface bold;
    private float density;

    private final TextView[] tabLabels = new TextView[3];
    private final View[] tabIndicators = new View[3];
    private final View[] pages = new View[3];
    private int current = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        density = getResources().getDisplayMetrics().density;
        regular = Typeface.create("rubik", Typeface.NORMAL);
        medium = Typeface.create("rubik-medium", Typeface.NORMAL);
        bold = Typeface.create("rubik", Typeface.BOLD);

        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        root.addView(appBar());
        root.addView(tabBar());

        FrameLayout content = new FrameLayout(this);
        content.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        pages[0] = page(pageSystem());
        pages[1] = page(pageDevice());
        pages[2] = page(pageZeros());
        for (View p : pages) {
            content.addView(p, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        }
        root.addView(content);

        int start = 0;
        if (getIntent() != null) {
            start = getIntent().getIntExtra("tab", 0);
        }
        select(Math.max(0, Math.min(pages.length - 1, start)));
        setContentView(root);
    }

    // ------------------------------------------------------------ app bar / tabs

    private View appBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bar.setBackground(bg);
        bar.setElevation(dp(4));
        bar.setPadding(dp(20), dp(14), dp(20), dp(14));

        TextView title = new TextView(this);
        title.setText(getString(R.string.app_name));
        title.setTextColor(TEXT);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTypeface(medium);
        bar.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("ZerOS " + roman() + " · " + version());
        subtitle.setTextColor(ACCENT);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        subtitle.setTypeface(medium);
        subtitle.setLetterSpacing(0.08f);
        subtitle.setPadding(0, dp(3), 0, 0);
        bar.addView(subtitle);

        return bar;
    }

    private View tabBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bar.setBackground(bg);
        bar.setElevation(dp(3));

        String[] labels = {
                getString(R.string.section_system),
                getString(R.string.section_device),
                getString(R.string.section_zeros),
        };

        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            LinearLayout tab = new LinearLayout(this);
            tab.setOrientation(LinearLayout.VERTICAL);
            tab.setLayoutParams(new LinearLayout.LayoutParams(
                    0, dp(48), 1f));
            tab.setBackground(ripple(0x33FFFFFF));

            TextView label = new TextView(this);
            label.setText(labels[i].toUpperCase(new Locale("ru", "RU")));
            label.setTextColor(MUTED);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            label.setTypeface(medium);
            label.setLetterSpacing(0.08f);
            label.setGravity(Gravity.CENTER);
            tab.addView(label, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

            View indicator = new View(this);
            indicator.setBackgroundColor(Color.TRANSPARENT);
            tab.addView(indicator, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(3)));

            tab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    select(index);
                }
            });

            tabLabels[i] = label;
            tabIndicators[i] = indicator;
            bar.addView(tab);
        }
        return bar;
    }

    private void select(int index) {
        if (index == current) {
            return;
        }
        for (int i = 0; i < pages.length; i++) {
            boolean active = i == index;
            tabLabels[i].setTextColor(active ? TEXT : MUTED);
            tabIndicators[i].setBackgroundColor(active ? ACCENT : Color.TRANSPARENT);
            if (active) {
                pages[i].setAlpha(0f);
                pages[i].setVisibility(View.VISIBLE);
                pages[i].animate().alpha(1f).setDuration(150).start();
            } else {
                pages[i].setVisibility(View.GONE);
            }
        }
        current = index;
    }

    // ------------------------------------------------------------------- pages

    private LinearLayout pageSystem() {
        LinearLayout body = body();
        body.addView(card(new String[][]{
                {getString(R.string.label_android), androidVersion()},
                {getString(R.string.label_security), securityPatch()},
                {getString(R.string.label_kernel), kernel()},
                {getString(R.string.label_build), buildId()},
                {getString(R.string.label_build_date), buildDate()},
        }), spacedCard());
        return body;
    }

    private LinearLayout pageDevice() {
        LinearLayout body = body();
        body.addView(card(new String[][]{
                {getString(R.string.label_model), model()},
                {getString(R.string.label_device), Build.DEVICE},
                {getString(R.string.label_platform), platform()},
                {getString(R.string.label_screen), screen()},
                {getString(R.string.label_memory), memory()},
                {getString(R.string.label_storage), storage()},
        }), spacedCard());
        return body;
    }

    private LinearLayout pageZeros() {
        LinearLayout body = body();
        body.addView(card(new String[][]{
                {getString(R.string.label_generation), generation()},
                {getString(R.string.label_version), version()},
                {getString(R.string.label_release), releaseType()},
                {getString(R.string.label_sdk), sdk()},
        }), spacedCard());
        body.addView(updatesButton());
        body.addView(footer());
        return body;
    }

    private ScrollView page(LinearLayout body) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(BG);
        scroll.addView(body, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return scroll;
    }

    private LinearLayout body() {
        LinearLayout b = new LinearLayout(this);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setPadding(dp(16), dp(16), dp(16), dp(24));
        return b;
    }

    // ------------------------------------------------------------------- cards

    private LinearLayout.LayoutParams spacedCard() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        return lp;
    }

    private View card(String[][] rows) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(8));
        bg.setColor(CARD);
        card.setBackground(bg);
        card.setElevation(dp(2));
        card.setPadding(dp(16), dp(4), dp(16), dp(4));

        for (int i = 0; i < rows.length; i++) {
            card.addView(row(rows[i][0], rows[i][1]));
            if (i < rows.length - 1) {
                View line = new View(this);
                line.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Math.max(1, Math.round(density))));
                line.setBackgroundColor(DIVIDER);
                card.addView(line);
            }
        }
        return card;
    }

    private View row(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(12), 0, dp(12));

        TextView l = new TextView(this);
        l.setText(label);
        l.setTextColor(MUTED);
        l.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        l.setTypeface(regular);
        row.addView(l);

        TextView v = new TextView(this);
        v.setText(value);
        v.setTextColor(TEXT);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        v.setTypeface(medium);
        v.setPadding(0, dp(3), 0, 0);
        v.setTextIsSelectable(true);
        row.addView(v);

        return row;
    }

    private View updatesButton() {
        TextView b = new TextView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(40));
        b.setLayoutParams(lp);
        b.setGravity(Gravity.CENTER);
        b.setText(getString(R.string.action_updates));
        b.setTextColor(Color.WHITE);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        b.setTypeface(medium);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(8));
        bg.setColor(ACCENT);
        b.setBackground(new RippleDrawable(
                ColorStateList.valueOf(0x40FFFFFF), bg, null));
        b.setClickable(true);
        b.setFocusable(true);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
        });
        return b;
    }

    private View footer() {
        TextView t = new TextView(this);
        t.setText(getString(R.string.footer));
        t.setTextColor(FOOTER);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        t.setTypeface(regular);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(24), 0, dp(8));
        return t;
    }

    private RippleDrawable ripple(int color) {
        return new RippleDrawable(ColorStateList.valueOf(color), null, null);
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

    // ----------------------------------------------------------------- helpers

    private int dp(float value) {
        return Math.round(value * density);
    }
}
