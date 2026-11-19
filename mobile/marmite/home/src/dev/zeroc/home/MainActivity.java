package dev.zeroc.home;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ZerOS Home 0.0.1 — рабочий стол ZerOS по макету Zero.C Design System 1.0.
 *
 * 0.0.1 — root-фикс: настоящая проверка su из фонового потока (раньше
 * смотрели в пустой кеш и Magisk не спрашивал); ниже — правки 1.1.
 *
 * Две страницы, листаются пальцем: стол (шапка, приветствие, карточка
 * устройства, плитки приложений, «Быстрое управление») и приложения.
 * Нижняя навигация общая для обеих страниц. Палитра макета: primary #FF6259,
 * surface #151519/#232327, background #090A0E, secondary #D6C2C1.
 * Шрифт Inter, логотип Redaction, «жидкое стекло» на авроре.
 */
public class MainActivity extends Activity {

    /* ------------------------------------------------------------- палитра */

    private static final int C_BG = 0xFF090A0E;
    private static final int C_SURFACE = 0xFF151519;
    private static final int C_SURFACE_2 = 0xFF232327;
    private static final int C_TEXT = 0xFFF5F2F1;
    private static final int C_DIM = 0xFF9A9498;
    private static final int C_MUTED = 0xFF6C666B;
    private static final int C_PRIMARY = 0xFFFF6259;
    private static final int C_PRIMARY_SOFT = 0x26FF6259;
    private static final int C_SECONDARY = 0xFFD6C2C1;
    private static final int C_RIPPLE = 0x24FFFFFF;

    /* Плитки приложений: мягкие фоны под белые/цветные иконки. */
    private static final int[] APP_TINTS = {
            0xFF3F6C9E, 0xFFFF6259, 0xFFA55A86, 0xFF6F6BA8,
            0xFF5E6167, 0xFFC08B3E, 0xFF4E8A72, 0xFF5A7285
    };

    private static final String PREFS_HOME = "home";
    private static final String PREFS_PLACEMENT = "placement";
    private static final String PREFS_NAME = "user_name";

    private final List<AppEntry> all = new ArrayList<>();
    private final Map<String, AppEntry> byKey = new LinkedHashMap<>();
    private final List<String> placement = new ArrayList<>();
    private final List<AppEntry> shown = new ArrayList<>();

    private final Handler ui = new Handler(Looper.getMainLooper());

    private ScrollView scroll;
    private PagesView pages;
    private final List<NavItem> navItems = new ArrayList<>();

    /* шапка и приветствие */
    private TextView date;
    private TextView greeting;
    private TextView userName;

    /* карточка устройства */
    private TextView deviceName;
    private TextView statTemp;
    private TextView statRam;
    private TextView statDisk;

    /* приложения на столе */
    private LinearLayout appsGrid;
    private TextView appsCounter;

    /* быстрое управление */
    private SeekBar brightnessBar;
    private TextView brightnessValue;
    private SeekBar volumeBar;
    private TextView volumeValue;
    private FrameLayout notifCircle;
    private ImageView notifIcon;
    private TextView notifCaption;
    private FrameLayout darkCircle;
    private ImageView darkIcon;
    private TextView darkCaption;
    private boolean syncing;

    /* страница приложений */
    private LinearLayout emptyBox;
    private GridView drawerGrid;
    private AppAdapter drawerAdapter;

    private Typeface fontRegular;
    private Typeface fontMedium;
    private Typeface fontSemiBold;
    private Typeface fontDisplay;

    private BroadcastReceiver pkgReceiver;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("EEEE, d MMMM", new Locale("ru"));

    private final Runnable clockTick = new Runnable() {
        @Override
        public void run() {
            updateClock();
            ui.postDelayed(this, 15000L);
        }
    };

    private final Runnable metricsTick = new Runnable() {
        @Override
        public void run() {
            updateMetrics();
            ui.postDelayed(this, 4000L);
        }
    };

    /** Пункт нижней навигации. */
    private static final class NavItem {
        LinearLayout box;
        ImageView icon;
        TextView label;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadFonts();
        loadPlacement();
        buildUi();
        updateClock();
        updateMetrics();
        loadApps();
        registerPackageReceiver();
        grantRootPermissions();
        maybeShowHomeHint();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ui.removeCallbacks(clockTick);
        ui.removeCallbacks(metricsTick);
        ui.post(clockTick);
        ui.post(metricsTick);
        syncControls();
        updateClock();
        updateMetrics();
        updateNav();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ui.removeCallbacks(clockTick);
        ui.removeCallbacks(metricsTick);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pkgReceiver != null) {
            try {
                unregisterReceiver(pkgReceiver);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (pages != null && pages.page() == PagesView.APPS) {
            pages.open(PagesView.HOME, true);
            return;
        }
        if (scroll != null && scroll.getScrollY() > 0) {
            scroll.smoothScrollTo(0, 0);
            return;
        }
        // остаёмся на месте
    }

    /* ------------------------------------------------------------------ UI */

    private void buildUi() {
        getWindow().setStatusBarColor(0x00000000);
        getWindow().setNavigationBarColor(C_BG);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(C_BG);
        frame.addView(new AuroraView(this), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        pages = new PagesView(this);
        pages.setPages(buildHomePage(), buildAppsPage());
        pages.setOnPageChangedListener(new PagesView.OnPageChanged() {
            @Override
            public void onPageChanged(int page) {
                updateNav();
            }
        });

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.addView(pages, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(buildNav());

        frame.addView(root, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(frame);

        // Первый кадр всегда с шапки, даже если системе вздумается отдать
        // фокус элементу внутри прокрутки.
        scroll.post(new Runnable() {
            @Override
            public void run() {
                if (scroll != null) {
                    scroll.scrollTo(0, 0);
                }
            }
        });
    }

    private View buildHomePage() {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setFitsSystemWindows(true);

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(8), dp(20), dp(16));

        content.addView(buildHeader());
        content.addView(buildGreeting());
        content.addView(buildDeviceCard());

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.topMargin = dp(18);
        content.addView(buildAppsSection(), cardLp);

        LinearLayout.LayoutParams quickLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        quickLp.topMargin = dp(18);
        content.addView(buildQuickControls(), quickLp);

        content.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showDesktopMenu();
                return true;
            }
        });

        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        column.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        column.setFocusableInTouchMode(true);
        column.requestFocus();
        return column;
    }

    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, dp(10), 0, 0);

        FrameLayout logo = new FrameLayout(this);
        logo.setBackground(roundRect(C_PRIMARY, 14));
        ImageView bolt = new ImageView(this);
        bolt.setImageResource(R.drawable.ic_bolt);
        bolt.setImageTintList(ColorStateList.valueOf(0xFFFFFFFF));
        FrameLayout.LayoutParams boltLp = new FrameLayout.LayoutParams(dp(18), dp(18));
        boltLp.gravity = Gravity.CENTER;
        logo.addView(bolt, boltLp);
        header.addView(logo, new LinearLayout.LayoutParams(dp(36), dp(36)));

        TextView brand = text("Zeros", 23, C_TEXT, fontDisplay);
        brand.setIncludeFontPadding(false);
        LinearLayout.LayoutParams brandLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        brandLp.leftMargin = dp(10);
        header.addView(brand, brandLp);

        TextView version = text("0.0.1", 10, 0xFF74DE8B, fontMedium);
        version.setIncludeFontPadding(false);
        version.setLetterSpacing(0.02f);
        version.setBackground(roundRect(0xFF17301F, 10));
        version.setPadding(dp(8), dp(3), dp(8), dp(3));
        LinearLayout.LayoutParams versionLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        versionLp.leftMargin = dp(8);
        header.addView(version, versionLp);

        View spacer = new View(this);
        header.addView(spacer, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView mark = text("ZERO.C", 9, C_MUTED, fontMedium);
        mark.setIncludeFontPadding(false);
        mark.setLetterSpacing(0.2f);
        header.addView(mark);

        return header;
    }

    private View buildGreeting() {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(24);
        block.setLayoutParams(lp);

        date = text("", 10, C_DIM, fontMedium);
        date.setIncludeFontPadding(false);
        date.setLetterSpacing(0.16f);
        date.setAllCaps(true);
        block.addView(date);

        greeting = text("Добрый вечер.", 30, C_TEXT, fontSemiBold);
        greeting.setIncludeFontPadding(false);
        LinearLayout.LayoutParams greetLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        greetLp.topMargin = dp(8);
        block.addView(greeting, greetLp);

        userName = text(prefsName(), 30, C_PRIMARY, fontSemiBold);
        userName.setIncludeFontPadding(false);
        userName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askName();
            }
        });
        block.addView(userName);

        return block;
    }

    private View buildDeviceCard() {
        LinearLayout card = card();

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);

        TextView label = text("ОБЗОР УСТРОЙСТВА", 9, C_DIM, fontMedium);
        label.setIncludeFontPadding(false);
        label.setLetterSpacing(0.18f);
        titles.addView(label);

        deviceName = text(deviceModel(), 17, C_TEXT, fontSemiBold);
        deviceName.setIncludeFontPadding(false);
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nameLp.topMargin = dp(4);
        titles.addView(deviceName, nameLp);

        TextView sub = text("Zero.C подключённое устройство", 11, C_DIM, fontRegular);
        sub.setIncludeFontPadding(false);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(3);
        titles.addView(sub, subLp);

        top.addView(titles, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        FrameLayout badge = new FrameLayout(this);
        badge.setBackground(roundRect(C_PRIMARY, 15));
        ImageView phone = new ImageView(this);
        phone.setImageResource(R.drawable.ic_phone);
        phone.setImageTintList(ColorStateList.valueOf(0xFFFFFFFF));
        FrameLayout.LayoutParams phoneLp = new FrameLayout.LayoutParams(dp(22), dp(22));
        phoneLp.gravity = Gravity.CENTER;
        badge.addView(phone, phoneLp);
        top.addView(badge, new LinearLayout.LayoutParams(dp(48), dp(48)));

        card.addView(top);

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams statsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statsLp.topMargin = dp(14);
        card.addView(stats, statsLp);

        statTemp = addStat(stats, R.drawable.ic_cpu, "ТЕМП. CPU", "—", 0);
        statRam = addStat(stats, R.drawable.ic_ram, "ОЗУ", "—", dp(8));
        statDisk = addStat(stats, R.drawable.ic_disk, "ПЗУ", "—", dp(8));

        return card;
    }

    private TextView addStat(LinearLayout row, int iconRes, String label, String value, int startMargin) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setBackground(roundRect(C_SURFACE_2, 16));
        tile.setPadding(dp(10), dp(9), dp(8), dp(9));

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setImageTintList(ColorStateList.valueOf(C_PRIMARY));
        tile.addView(icon, new LinearLayout.LayoutParams(dp(15), dp(15)));

        TextView val = text(value, 19, C_TEXT, fontSemiBold);
        val.setIncludeFontPadding(false);
        val.setSingleLine(true);
        LinearLayout.LayoutParams valLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        valLp.topMargin = dp(7);
        tile.addView(val, valLp);

        TextView cap = text(label, 8.5f, C_DIM, fontMedium);
        cap.setIncludeFontPadding(false);
        cap.setLetterSpacing(0.1f);
        cap.setSingleLine(true);
        cap.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams capLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        capLp.topMargin = dp(2);
        tile.addView(cap, capLp);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        if (startMargin > 0) {
            lp.leftMargin = startMargin;
        }
        row.addView(tile, lp);
        return val;
    }

    private View buildAppsSection() {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setPadding(dp(2), 0, dp(2), 0);

        TextView title = text("Приложения", 18, C_TEXT, fontSemiBold);
        titleRow.addView(title, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView allBtn = text("Все", 12, C_DIM, fontMedium);
        titleRow.addView(allBtn);

        ImageView arrow = new ImageView(this);
        arrow.setImageResource(R.drawable.ic_arrow);
        arrow.setImageTintList(ColorStateList.valueOf(C_DIM));
        LinearLayout.LayoutParams arrowLp = new LinearLayout.LayoutParams(dp(13), dp(13));
        arrowLp.leftMargin = dp(4);
        titleRow.addView(arrow, arrowLp);

        View.OnClickListener openAll = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openApps();
            }
        };
        allBtn.setOnClickListener(openAll);
        arrow.setOnClickListener(openAll);

        block.addView(titleRow);

        appsGrid = new LinearLayout(this);
        appsGrid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams gridLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gridLp.topMargin = dp(10);
        block.addView(appsGrid, gridLp);

        return block;
    }

    private View buildQuickControls() {
        LinearLayout card = card();

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = text("Быстрое управление", 18, C_TEXT, fontSemiBold);
        titleRow.addView(title, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        ImageView tune = new ImageView(this);
        tune.setImageResource(R.drawable.ic_tune);
        tune.setImageTintList(ColorStateList.valueOf(C_PRIMARY));
        titleRow.addView(tune, new LinearLayout.LayoutParams(dp(19), dp(19)));

        card.addView(titleRow);

        card.addView(buildSlider(R.drawable.ic_bolt, "Яркость", true));
        card.addView(buildSlider(R.drawable.ic_volume, "Громкость", false));

        LinearLayout.LayoutParams togglesLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        togglesLp.topMargin = dp(10);
        card.addView(buildCircleToggles(), togglesLp);

        return card;
    }

    private View buildSlider(int iconRes, String label, final boolean isBrightness) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(14);
        block.setLayoutParams(lp);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setImageTintList(ColorStateList.valueOf(C_PRIMARY));
        row.addView(icon, new LinearLayout.LayoutParams(dp(16), dp(16)));

        TextView name = text(label, 13, C_TEXT, fontMedium);
        name.setIncludeFontPadding(false);
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        nameLp.leftMargin = dp(8);
        row.addView(name, nameLp);

        TextView value = text("0%", 13, C_DIM, fontRegular);
        value.setIncludeFontPadding(false);
        value.setFontFeatureSettings("tnum");
        row.addView(value);

        block.addView(row);

        SeekBar bar = new SeekBar(this);
        bar.setMax(100);
        bar.setPadding(0, 0, 0, 0);
        bar.setSplitTrack(false);
        // Слайдер не должен забирать фокус: иначе ScrollView при старте
        // прокручивается к нему и шапка с приветствием уезжает вверх.
        bar.setFocusable(false);
        bar.setFocusableInTouchMode(false);
        bar.setProgressTintList(ColorStateList.valueOf(0xFFFFFFFF));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(0x26FFFFFF));
        bar.setThumbTintList(ColorStateList.valueOf(0xFFFFFFFF));
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(34));
        barLp.topMargin = dp(2);
        block.addView(bar, barLp);

        if (isBrightness) {
            brightnessBar = bar;
            brightnessValue = value;
        } else {
            volumeBar = bar;
            volumeValue = value;
        }

        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                TextView target = isBrightness ? brightnessValue : volumeValue;
                if (target != null) {
                    target.setText(progress + "%");
                }
                if (!fromUser || syncing) {
                    return;
                }
                if (!isBrightness) {
                    applyVolume(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Яркость пишем один раз, на отпускании: через root это запуск
                // процесса, дёргать его на каждый пиксель нельзя.
                if (isBrightness && !syncing) {
                    applyBrightness(seekBar.getProgress());
                }
            }
        });

        return block;
    }

    /** Два круглых переключателя: уведомления и тёмная тема. */
    private View buildCircleToggles() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, 0);

        row.addView(circleToggle(R.drawable.ic_bell, "Уведомления", true),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(circleToggle(R.drawable.ic_moon, "Тёмная тема", false),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private View circleToggle(int iconRes, String label, final boolean isNotifications) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(0, dp(8), 0, dp(2));

        FrameLayout circle = new FrameLayout(this);
        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(dp(22), dp(22));
        iconLp.gravity = Gravity.CENTER;
        circle.addView(icon, iconLp);
        box.addView(circle, new LinearLayout.LayoutParams(dp(58), dp(58)));

        TextView caption = text(label, 11, C_DIM, fontMedium);
        caption.setIncludeFontPadding(false);
        caption.setSingleLine(true);
        caption.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams capLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        capLp.topMargin = dp(7);
        box.addView(caption, capLp);

        if (isNotifications) {
            notifCircle = circle;
            notifIcon = icon;
            notifCaption = caption;
        } else {
            darkCircle = circle;
            darkIcon = icon;
            darkCaption = caption;
        }
        styleCircle(circle, icon, caption, false);

        box.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isNotifications) {
                    toggleNotifications();
                } else {
                    toggleDark();
                }
            }
        });

        return box;
    }

    private void styleCircle(FrameLayout circle, ImageView icon, TextView caption, boolean on) {
        if (circle == null || icon == null) {
            return;
        }
        if (on) {
            circle.setBackground(roundRect(C_PRIMARY, 29));
            icon.setImageTintList(ColorStateList.valueOf(0xFFFFFFFF));
            if (caption != null) {
                caption.setTextColor(C_TEXT);
            }
        } else {
            GradientDrawable off = new GradientDrawable();
            off.setShape(GradientDrawable.OVAL);
            off.setColor(0x14FFFFFF);
            off.setStroke(Math.max(1, dp(1.4f)), 0x40FFFFFF);
            circle.setBackground(off);
            icon.setImageTintList(ColorStateList.valueOf(C_PRIMARY));
            if (caption != null) {
                caption.setTextColor(C_DIM);
            }
        }
    }

    private View buildNav() {
        FrameLayout wrap = new FrameLayout(this);
        wrap.setPadding(dp(14), dp(6), dp(14), dp(10));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackground(glass(0xF2141418, 24, 0x2E));
        bar.setPadding(dp(6), dp(4), dp(6), dp(4));

        navItems.clear();
        bar.addView(navItem(R.drawable.ic_home, "Главная", 0));
        bar.addView(navItem(R.drawable.ic_apps, "Приложения", 1));
        bar.addView(navItem(R.drawable.ic_person, "Профиль", -1));

        wrap.addView(bar, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(58)));

        updateNav();
        return wrap;
    }

    private View navItem(int iconRes, String label, final int target) {
        NavItem item = new NavItem();

        item.box = new LinearLayout(this);
        item.box.setOrientation(LinearLayout.VERTICAL);
        item.box.setGravity(Gravity.CENTER);
        item.box.setPadding(0, dp(7), 0, dp(5));
        item.box.setBackground(rippleOnly(16));

        item.icon = new ImageView(this);
        item.icon.setImageResource(iconRes);
        item.box.addView(item.icon, new LinearLayout.LayoutParams(dp(20), dp(20)));

        item.label = text(label, 9.5f, C_DIM, fontMedium);
        item.label.setIncludeFontPadding(false);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textLp.topMargin = dp(3);
        item.box.addView(item.label, textLp);

        navItems.add(item);

        item.box.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (target < 0) {
                    showProfile();
                    return;
                }
                if (pages != null) {
                    pages.open(target, true);
                }
            }
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        lp.setMargins(dp(2), 0, dp(2), 0);
        item.box.setLayoutParams(lp);
        return item.box;
    }

    private void updateNav() {
        int current = pages == null ? 0 : pages.page();
        for (int i = 0; i < navItems.size(); i++) {
            NavItem item = navItems.get(i);
            boolean active = i == current;
            int tint = active ? C_PRIMARY : 0xFF8B8589;
            item.icon.setImageTintList(ColorStateList.valueOf(tint));
            item.label.setTextColor(tint);
            item.box.setBackground(active ? roundRect(C_PRIMARY_SOFT, 16) : rippleOnly(16));
        }
    }

    /* --------------------------------------------------- страница приложений */

    private View buildAppsPage() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(0xF5090A0E);
        page.setFitsSystemWindows(true);
        page.setPadding(dp(20), dp(8), dp(20), 0);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setPadding(0, dp(16), 0, dp(4));

        TextView title = text("Приложения", 26, C_TEXT, fontSemiBold);
        title.setIncludeFontPadding(false);
        titleRow.addView(title, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        appsCounter = text("", 12, C_DIM, fontMedium);
        appsCounter.setIncludeFontPadding(false);
        titleRow.addView(appsCounter);

        page.addView(titleRow);

        TextView hint = text("листните вправо — рабочий стол", 11, C_MUTED, fontRegular);
        hint.setIncludeFontPadding(false);
        page.addView(hint);

        FrameLayout body = new FrameLayout(this);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        bodyLp.topMargin = dp(12);
        page.addView(body, bodyLp);

        drawerGrid = new GridView(this);
        drawerGrid.setNumColumns(4);
        drawerGrid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        drawerGrid.setHorizontalSpacing(dp(6));
        drawerGrid.setVerticalSpacing(dp(16));
        drawerGrid.setPadding(0, dp(2), 0, dp(12));
        drawerGrid.setClipToPadding(false);
        drawerGrid.setSelector(new ColorDrawable(0x00000000));
        drawerGrid.setVerticalScrollBarEnabled(false);

        drawerAdapter = new AppAdapter(this, shown, fontRegular, fontMedium);
        drawerGrid.setAdapter(drawerAdapter);
        drawerGrid.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
                launch(shown.get(position));
            }
        });
        drawerGrid.setOnItemLongClickListener(new android.widget.AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
                showAppMenu(shown.get(position), false);
                return true;
            }
        });

        body.addView(drawerGrid, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        emptyBox = new LinearLayout(this);
        emptyBox.setOrientation(LinearLayout.VERTICAL);
        emptyBox.setGravity(Gravity.CENTER);
        emptyBox.setVisibility(View.GONE);

        TextView empty = text("Приложений нет", 16, C_TEXT, fontMedium);
        empty.setGravity(Gravity.CENTER);
        emptyBox.addView(empty);

        TextView emptyHint = text("Установите что-нибудь — оно появится здесь", 13, C_DIM, fontRegular);
        emptyHint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintLp.topMargin = dp(6);
        emptyHint.setLayoutParams(hintLp);
        emptyBox.addView(emptyHint);

        body.addView(emptyBox, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        return page;
    }

    private void openApps() {
        if (pages != null) {
            pages.open(PagesView.APPS, true);
        }
    }

    /* ------------------------------------------------------------- приложения */

    private void loadApps() {
        final Context appContext = getApplicationContext();
        Thread loader = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final PackageManager pm = appContext.getPackageManager();
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_LAUNCHER);
                    List<ResolveInfo> resolved = pm.queryIntentActivities(intent, 0);

                    final Map<String, AppEntry> unique = new LinkedHashMap<>();
                    for (ResolveInfo info : resolved) {
                        ActivityInfo activity = info.activityInfo;
                        if (activity == null) {
                            continue;
                        }
                        if (appContext.getPackageName().equals(activity.packageName)) {
                            continue;
                        }

                        AppEntry entry = new AppEntry();
                        entry.packageName = activity.packageName;
                        entry.activityName = activity.name;
                        entry.label = String.valueOf(info.loadLabel(pm));
                        entry.icon = info.loadIcon(pm);
                        unique.put(keyOf(entry), entry);
                    }

                    final List<AppEntry> loaded = new ArrayList<>(unique.values());
                    Collections.sort(loaded, new Comparator<AppEntry>() {
                        @Override
                        public int compare(AppEntry a, AppEntry b) {
                            return a.label.compareToIgnoreCase(b.label);
                        }
                    });

                    ui.post(new Runnable() {
                        @Override
                        public void run() {
                            all.clear();
                            all.addAll(loaded);
                            byKey.clear();
                            byKey.putAll(unique);
                            syncPlacement();
                            applyFilter();
                            renderApps();
                        }
                    });
                } catch (Throwable t) {
                    android.util.Log.e("ZerOS", "loadApps failed", t);
                }
            }
        });
        loader.setName("zeroc-loader");
        loader.start();
    }

    private String keyOf(AppEntry entry) {
        return entry.packageName + "/" + entry.activityName;
    }

    private void syncPlacement() {
        List<String> next = new ArrayList<>();
        for (String key : placement) {
            if (byKey.containsKey(key) && !next.contains(key)) {
                next.add(key);
            }
        }
        for (AppEntry entry : all) {
            String key = keyOf(entry);
            if (!next.contains(key)) {
                next.add(key);
            }
        }

        if (!next.equals(placement)) {
            placement.clear();
            placement.addAll(next);
            savePlacement();
        }
    }

    private void loadPlacement() {
        SharedPreferences prefs = getSharedPreferences(PREFS_HOME, MODE_PRIVATE);
        String stored = prefs.getString(PREFS_PLACEMENT, "");
        placement.clear();
        if (stored == null || stored.isEmpty()) {
            return;
        }
        for (String line : stored.split("\n")) {
            if (!line.trim().isEmpty()) {
                placement.add(line.trim());
            }
        }
    }

    private void savePlacement() {
        StringBuilder sb = new StringBuilder();
        for (String key : placement) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(key);
        }
        getSharedPreferences(PREFS_HOME, MODE_PRIVATE)
                .edit()
                .putString(PREFS_PLACEMENT, sb.toString())
                .apply();
    }

    /** Восемь плиток на дашборде: первые места раскладки. */
    private void renderApps() {
        if (appsGrid == null) {
            return;
        }
        appsGrid.removeAllViews();

        List<AppEntry> picks = new ArrayList<>();
        for (String key : placement) {
            AppEntry entry = byKey.get(key);
            if (entry != null && picks.size() < 8) {
                picks.add(entry);
            }
        }

        for (int row = 0; row < 2; row++) {
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            if (row > 0) {
                LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lineLp.topMargin = dp(12);
                line.setLayoutParams(lineLp);
            }

            for (int col = 0; col < 4; col++) {
                int index = row * 4 + col;
                if (index < picks.size()) {
                    line.addView(appCell(picks.get(index), index));
                } else {
                    View filler = new View(this);
                    line.addView(filler, new LinearLayout.LayoutParams(
                            0, dp(72), 1f));
                }
            }
            appsGrid.addView(line);
        }
    }

    private View appCell(final AppEntry entry, int index) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);

        FrameLayout tile = new FrameLayout(this);
        tile.setBackground(roundRect(APP_TINTS[index % APP_TINTS.length], 17));

        ImageView icon = new ImageView(this);
        icon.setImageDrawable(entry.icon);
        int pad = dp(12);
        icon.setPadding(pad, pad, pad, pad);
        FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(dp(54), dp(54));
        iconLp.gravity = Gravity.CENTER;
        tile.addView(icon, iconLp);

        cell.addView(tile, new LinearLayout.LayoutParams(dp(54), dp(54)));

        TextView label = text(entry.label, 10, C_TEXT, fontMedium);
        label.setIncludeFontPadding(false);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        labelLp.topMargin = dp(6);
        cell.addView(label, labelLp);

        cell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launch(entry);
            }
        });
        cell.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showAppMenu(entry, true);
                return true;
            }
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        cell.setLayoutParams(lp);
        return cell;
    }

    /** Список приложений больше не фильтруется — поиска нет, страница просто листается. */
    private void applyFilter() {
        if (drawerAdapter == null) {
            return;
        }
        shown.clear();
        shown.addAll(all);
        drawerAdapter.notifyDataSetChanged();

        if (appsCounter != null) {
            appsCounter.setText(String.valueOf(shown.size()));
        }

        boolean isEmpty = shown.isEmpty();
        if (drawerGrid != null) {
            drawerGrid.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
        if (emptyBox != null) {
            emptyBox.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
    }

    private void launch(AppEntry entry) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setClassName(entry.packageName, entry.activityName);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Не запускается: " + entry.label, Toast.LENGTH_SHORT).show();
        }
    }

    private void showAppMenu(final AppEntry entry, final boolean onDesktop) {
        final List<String> items = new ArrayList<>();
        items.add("Открыть");
        items.add(onDesktop ? "Убрать с рабочего стола" : "Добавить на рабочий стол");
        items.add("О приложении");
        if (!isSystemApp(entry.packageName)) {
            items.add("Удалить");
        }

        new AlertDialog.Builder(this, R.style.ZerOS_Dialog)
                .setTitle(entry.label)
                .setItems(items.toArray(new String[0]), new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        String choice = items.get(which);
                        if ("Открыть".equals(choice)) {
                            launch(entry);
                        } else if ("Убрать с рабочего стола".equals(choice)) {
                            removeFromDesktop(entry);
                        } else if ("Добавить на рабочий стол".equals(choice)) {
                            addToDesktop(entry);
                        } else if ("О приложении".equals(choice)) {
                            openAppInfo(entry.packageName);
                        } else if ("Удалить".equals(choice)) {
                            try {
                                startActivity(new Intent(Intent.ACTION_DELETE,
                                        Uri.parse("package:" + entry.packageName)));
                            } catch (Exception e) {
                                Toast.makeText(MainActivity.this, "Не удалось открыть удаление",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                })
                .show();
    }

    private void showDesktopMenu() {
        final String[] items = {"Все приложения", "Профиль устройства", "Обои и стиль", "Настройки Android"};
        new AlertDialog.Builder(this, R.style.ZerOS_Dialog)
                .setItems(items, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        if (which == 0) {
                            openApps();
                        } else if (which == 1) {
                            showProfile();
                        } else if (which == 2) {
                            openWallpaper();
                        } else {
                            openSettings();
                        }
                    }
                })
                .show();
    }

    private void showProfile() {
        String model = deviceModel();
        String message = model
                + "\nAndroid " + android.os.Build.VERSION.RELEASE
                + " · API " + android.os.Build.VERSION.SDK_INT
                + "\nZerOS Home 0.0.1 · Zero.C Design System"
                + (Root.available() ? "\nroot: есть" : "\nroot: нет");
        new AlertDialog.Builder(this, R.style.ZerOS_Dialog)
                .setTitle("Профиль")
                .setMessage(message)
                .setPositiveButton("Настройки Android", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        openSettings();
                    }
                })
                .setNegativeButton("Закрыть", null)
                .show();
    }

    private void addToDesktop(AppEntry entry) {
        String key = keyOf(entry);
        if (placement.contains(key)) {
            Toast.makeText(this, "Уже на рабочем столе", Toast.LENGTH_SHORT).show();
            return;
        }
        placement.add(key);
        savePlacement();
        renderApps();
        Toast.makeText(this, "Добавлено на рабочий стол", Toast.LENGTH_SHORT).show();
    }

    private void removeFromDesktop(AppEntry entry) {
        if (placement.remove(keyOf(entry))) {
            savePlacement();
            renderApps();
        }
    }

    private boolean isSystemApp(String packageName) {
        try {
            ApplicationInfo info = getPackageManager().getApplicationInfo(packageName, 0);
            return (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                    || (info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void openAppInfo(String packageName) {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + packageName)));
        } catch (Exception e) {
            Toast.makeText(this, "Настройки приложения недоступны", Toast.LENGTH_SHORT).show();
        }
    }

    private void openSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        } catch (Exception e) {
            Toast.makeText(this, "Настройки недоступны", Toast.LENGTH_SHORT).show();
        }
    }

    private void openDisplaySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_DISPLAY_SETTINGS));
        } catch (Exception e) {
            openSettings();
        }
    }

    private void openWallpaper() {
        try {
            Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Обои недоступны", Toast.LENGTH_SHORT).show();
        }
    }

    /* ------------------------------------------------- быстрое управление */

    /** Один раз просим root выдать себе то, что в настройках Android выдать нельзя. */
    private void grantRootPermissions() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Именно здесь Magisk впервые показывает запрос на root:
                // available() только читает кеш, su запускает check().
                if (!Root.check()) {
                    return;
                }
                Root.grantWriteSettings(getPackageName());
                Root.grantNotificationPolicy(getPackageName());
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        syncControls();
                    }
                });
            }
        }, "zeroc-root-grant").start();
    }

    private void syncControls() {
        syncing = true;
        try {
            int brightness = 128;
            try {
                brightness = Settings.System.getInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS, 128);
            } catch (Exception ignored) {
            }
            int brightnessPercent = Math.round(brightness * 100f / 255f);
            if (brightnessBar != null) {
                brightnessBar.setProgress(brightnessPercent);
            }
            if (brightnessValue != null) {
                brightnessValue.setText(brightnessPercent + "%");
            }

            AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audio != null && volumeBar != null) {
                int max = Math.max(1, audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
                int current = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
                int percent = Math.round(current * 100f / max);
                volumeBar.setProgress(percent);
                if (volumeValue != null) {
                    volumeValue.setText(percent + "%");
                }
            }

            styleCircle(notifCircle, notifIcon, notifCaption, isNotificationsOn());
            styleCircle(darkCircle, darkIcon, darkCaption, isDarkMode());
        } finally {
            syncing = false;
        }
    }

    private void applyBrightness(final int percent) {
        final int value = Math.round(percent * 255f / 100f);

        if (Settings.System.canWrite(this)) {
            try {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS, value);
            } catch (Exception ignored) {
            }
            return;
        }

        // Настройки не дают выдать разрешение вручную — делаем через root.
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!Root.check()) {
                    ui.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,
                                    "Нет root: разрешите «Изменение системных настроек»",
                                    Toast.LENGTH_SHORT).show();
                            try {
                                startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                        Uri.parse("package:" + getPackageName())));
                            } catch (Exception ignored) {
                            }
                        }
                    });
                    return;
                }
                // Заодно выдаём appop, чтобы следующие правки шли без su.
                Root.grantWriteSettings(getPackageName());
                Root.setBrightness(value);
            }
        }, "zeroc-brightness").start();
    }

    private void applyVolume(int percent) {
        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audio == null) {
            return;
        }
        try {
            int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, Math.round(percent * max / 100f), 0);
        } catch (Exception ignored) {
        }
    }

    private boolean isDarkMode() {
        return (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private void toggleDark() {
        final boolean dark = isDarkMode();
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!Root.check()) {
                    ui.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,
                                    "Нет root — тема меняется в настройках экрана",
                                    Toast.LENGTH_SHORT).show();
                            openDisplaySettings();
                        }
                    });
                    return;
                }
                Root.setNightMode(!dark);
                // Даём системе время применить конфигурацию и перечитать её.
                ui.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        syncControls();
                    }
                }, 900L);
            }
        }, "zeroc-theme").start();
    }

    private boolean isNotificationsOn() {
        try {
            int zen = Settings.Global.getInt(getContentResolver(), "zen_mode", -1);
            if (zen >= 0 && Root.available()) {
                return zen == 0;
            }
        } catch (Exception ignored) {
        }

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) {
            return true;
        }
        try {
            if (!nm.isNotificationPolicyAccessGranted()) {
                return true;
            }
            return nm.getCurrentInterruptionFilter() == NotificationManager.INTERRUPTION_FILTER_ALL;
        } catch (Exception e) {
            return true;
        }
    }

    private void toggleNotifications() {
        final NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (nm != null && nm.isNotificationPolicyAccessGranted()) {
            try {
                boolean nowOn = nm.getCurrentInterruptionFilter() == NotificationManager.INTERRUPTION_FILTER_ALL;
                nm.setInterruptionFilter(nowOn
                        ? NotificationManager.INTERRUPTION_FILTER_PRIORITY
                        : NotificationManager.INTERRUPTION_FILTER_ALL);
                syncControls();
                return;
            } catch (Exception ignored) {
            }
        }

        final boolean nowOn = isNotificationsOn();
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!Root.check()) {
                    ui.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,
                                    "Нет root: разрешите «Доступ к режиму Не беспокоить»",
                                    Toast.LENGTH_SHORT).show();
                            try {
                                startActivity(new Intent(
                                        Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
                            } catch (Exception ignored) {
                            }
                        }
                    });
                    return;
                }
                Root.setZenMode(!nowOn);
                ui.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        syncControls();
                    }
                }, 600L);
            }
        }, "zeroc-dnd").start();
    }

    /* ------------------------------------------------------------- метрики */

    private void updateClock() {
        if (date != null) {
            String text = dateFormat.format(new Date());
            if (!text.isEmpty()) {
                text = Character.toUpperCase(text.charAt(0)) + text.substring(1);
            }
            date.setText(text);
        }
        if (greeting != null) {
            greeting.setText(greetingForHour());
        }
    }

    private String greetingForHour() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour < 5) {
            return "Доброй ночи.";
        }
        if (hour < 12) {
            return "Доброе утро.";
        }
        if (hour < 18) {
            return "Добрый день.";
        }
        return "Добрый вечер.";
    }

    private void updateMetrics() {
        if (statTemp != null) {
            int temp = readCpuTemp();
            statTemp.setText(temp > 0 ? temp + "\u00B0" : "—");
        }

        if (statRam != null) {
            try {
                android.app.ActivityManager am =
                        (android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                android.app.ActivityManager.MemoryInfo info = new android.app.ActivityManager.MemoryInfo();
                am.getMemoryInfo(info);
                statRam.setText(gb(info.totalMem));
            } catch (Exception e) {
                statRam.setText("—");
            }
        }

        if (statDisk != null) {
            try {
                StatFs fs = new StatFs(Environment.getDataDirectory().getAbsolutePath());
                long free = fs.getAvailableBytes();
                statDisk.setText(gb(free));
            } catch (Exception e) {
                statDisk.setText("—");
            }
        }

        if (deviceName != null) {
            deviceName.setText(deviceModel());
        }
    }

    private String gb(long bytes) {
        double value = bytes / 1073741824d;
        if (value >= 100) {
            return Math.round(value) + " ГБ";
        }
        String text = String.format(Locale.US, "%.1f", value);
        return text + " ГБ";
    }

    /** Температура из термозоны, если ядро и SELinux разрешают чтение. */
    private int readCpuTemp() {
        File base = new File("/sys/class/thermal");
        File[] zones = base.listFiles();
        if (zones == null) {
            return 0;
        }
        int best = 0;
        for (File zone : zones) {
            if (zone == null || !zone.getName().startsWith("thermal_zone")) {
                continue;
            }
            try {
                File typeFile = new File(zone, "type");
                String type = readFirstLine(typeFile);
                if (type == null) {
                    continue;
                }
                String lower = type.toLowerCase(Locale.US);
                if (!(lower.contains("cpu") || lower.contains("soc") || lower.contains("tsens")
                        || lower.contains("apc"))) {
                    continue;
                }
                String raw = readFirstLine(new File(zone, "temp"));
                if (raw == null) {
                    continue;
                }
                int value = Integer.parseInt(raw.trim());
                if (value > 1000) {
                    value = value / 1000;
                }
                if (value > 0 && value < 120 && value > best) {
                    best = value;
                }
            } catch (Exception ignored) {
            }
        }
        return best;
    }

    private String readFirstLine(File file) {
        try {
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(new java.io.FileInputStream(file)));
            try {
                return reader.readLine();
            } finally {
                reader.close();
            }
        } catch (Exception e) {
            return null;
        }
    }

    private String deviceModel() {
        String model = android.os.Build.MODEL;
        String vendor = android.os.Build.MANUFACTURER;
        if (model == null || model.isEmpty()) {
            return "Устройство";
        }
        if (vendor == null || vendor.isEmpty() || "unknown".equalsIgnoreCase(vendor)) {
            return model;
        }
        if (model.toLowerCase(Locale.US).startsWith(vendor.toLowerCase(Locale.US))) {
            return model;
        }
        return vendor + " " + model;
    }

    /* ------------------------------------------------------------- системное */

    private void registerPackageReceiver() {
        pkgReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                loadApps();
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        registerReceiver(pkgReceiver, filter);
    }

    private void maybeShowHomeHint() {
        SharedPreferences prefs = getSharedPreferences(PREFS_HOME, MODE_PRIVATE);
        if (prefs.getBoolean("home_hint", false)) {
            return;
        }
        prefs.edit().putBoolean("home_hint", true).apply();

        new AlertDialog.Builder(this, R.style.ZerOS_Dialog)
                .setTitle("ZerOS Home 0.0.1")
                .setMessage("Сделать ZerOS Home домашним экраном по умолчанию?")
                .setPositiveButton("Открыть настройки", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        try {
                            startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
                        } catch (Exception e) {
                            openSettings();
                        }
                    }
                })
                .setNegativeButton("Позже", null)
                .show();
    }

    private String prefsName() {
        SharedPreferences prefs = getSharedPreferences(PREFS_HOME, MODE_PRIVATE);
        return prefs.getString(PREFS_NAME, "SillyTech.");
    }

    private void askName() {
        final EditText input = new EditText(this);
        input.setText(prefsName());
        input.setSingleLine(true);
        input.setTextColor(C_TEXT);
        input.setHintTextColor(C_MUTED);
        input.setTypeface(fontRegular);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setPadding(dp(18), dp(12), dp(18), dp(12));
        input.setBackground(glassRipple(0x1AFFFFFF, 16, 0x45));

        FrameLayout wrap = new FrameLayout(this);
        wrap.setPadding(dp(20), dp(8), dp(20), 0);
        wrap.addView(input, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        new AlertDialog.Builder(this, R.style.ZerOS_Dialog)
                .setTitle("Имя на рабочем столе")
                .setView(wrap)
                .setPositiveButton("Сохранить", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        String value = input.getText().toString().trim();
                        if (value.isEmpty()) {
                            value = "SillyTech.";
                        }
                        getSharedPreferences(PREFS_HOME, MODE_PRIVATE)
                                .edit()
                                .putString(PREFS_NAME, value)
                                .apply();
                        if (userName != null) {
                            userName.setText(value);
                        }
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    /* ---------------------------------------------------------------- утилиты */

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(glass(0xE6151519, 24, 0x2E));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(18);
        card.setLayoutParams(lp);
        return card;
    }

    private void loadFonts() {
        fontRegular = assetFont("fonts/inter_regular.ttf", Typeface.NORMAL);
        fontMedium = assetFont("fonts/inter_medium.ttf", Typeface.BOLD);
        fontSemiBold = assetFont("fonts/inter_semibold.ttf", Typeface.BOLD);
        fontDisplay = assetFont("fonts/redaction.ttf", Typeface.BOLD);
    }

    private Typeface assetFont(String path, int fallbackStyle) {
        try {
            return Typeface.createFromAsset(getAssets(), path);
        } catch (Exception e) {
            return Typeface.create("sans-serif", fallbackStyle);
        }
    }

    private TextView text(String value, float sizeSp, int color, Typeface typeface) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setTypeface(typeface);
        return view;
    }

    private GradientDrawable roundRect(int color, float radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private Drawable glass(int fill, float radiusDp, int edgeAlpha) {
        return new GlassDrawable(fill, dp(radiusDp), Math.max(1f, dp(0.7f)), edgeAlpha);
    }

    private Drawable glassRipple(int fill, float radiusDp, int edgeAlpha) {
        GradientDrawable mask = roundRect(0xFFFFFFFF, radiusDp);
        return new RippleDrawable(ColorStateList.valueOf(C_RIPPLE),
                glass(fill, radiusDp, edgeAlpha), mask);
    }

    private Drawable rippleOnly(float radiusDp) {
        GradientDrawable mask = roundRect(0xFFFFFFFF, radiusDp);
        return new RippleDrawable(ColorStateList.valueOf(C_RIPPLE), null, mask);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int statusBarHeight() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (id > 0) {
            return getResources().getDimensionPixelSize(id);
        }
        return dp(24);
    }
}
