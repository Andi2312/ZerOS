/*
 * ZerOS: Windows Phone style list of all apps.
 *
 * The list lives in its own activity inside the launcher task, so Home and the
 * Start button return to the workspace without any extra plumbing.
 */
package com.android.launcher3.zer;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.R;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ZerAppListActivity extends Activity {

    public static final String EXTRA_SEARCH = "com.android.launcher3.zer.extra.SEARCH";

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final List<ZerAppListAdapter.Entry> mAllApps = new ArrayList<>();
    private final LauncherApps.Callback mCallback = new LauncherApps.Callback() {
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

    private LauncherApps mLauncherApps;
    private ZerAppListAdapter mAdapter;
    private RecyclerView mList;
    private ZerLetterBar mLetterBar;
    private View mSearchBox;
    private EditText mSearch;
    private View mEmpty;
    private String mQuery = "";
    private boolean mLoaded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.zer_app_list);

        mLauncherApps = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);

        mList = findViewById(R.id.zer_apps_list);
        mLetterBar = findViewById(R.id.zer_letter_bar);
        mSearchBox = findViewById(R.id.zer_search_box);
        mSearch = findViewById(R.id.zer_search_input);
        mEmpty = findViewById(R.id.zer_empty);

        mAdapter = new ZerAppListAdapter(this, this::launch);
        mList.setLayoutManager(new LinearLayoutManager(this));
        mList.setAdapter(mAdapter);
        mList.setItemAnimator(null);

        mLetterBar.setListener(this::jumpToLetter);

        findViewById(R.id.zer_nav_back).setOnClickListener(v -> onBackPressed());
        findViewById(R.id.zer_nav_home).setOnClickListener(v -> finish());
        findViewById(R.id.zer_nav_search).setOnClickListener(v -> showSearch());

        mSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mQuery = s.toString().trim();
                rebuild();
            }
        });

        applyImmersiveMode();
        loadApps();

        if (getIntent().getBooleanExtra(EXTRA_SEARCH, false)) {
            showSearch();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mLauncherApps != null) {
            mLauncherApps.registerCallback(mCallback);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mLauncherApps != null) {
            mLauncherApps.unregisterCallback(mCallback);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mExecutor.shutdownNow();
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
        if (mSearchBox.getVisibility() == View.VISIBLE) {
            hideSearch();
            return;
        }
        super.onBackPressed();
    }

    private void applyImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private void showSearch() {
        mSearchBox.setVisibility(View.VISIBLE);
        mSearch.requestFocus();
        mSearch.post(() -> {
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(mSearch, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    private void hideSearch() {
        mSearch.setText("");
        mSearchBox.setVisibility(View.GONE);
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(mSearch.getWindowToken(), 0);
        }
    }

    private void loadApps() {
        final LauncherApps launcherApps = mLauncherApps;
        final int density = getResources().getDisplayMetrics().densityDpi;
        if (launcherApps == null) {
            return;
        }
        mExecutor.execute(() -> {
            List<UserHandle> users = new ArrayList<>();
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    users.addAll(launcherApps.getProfiles());
                }
            } catch (Exception ignored) {
                // Fall back to the current user below.
            }
            if (users.isEmpty()) {
                users.add(Process.myUserHandle());
            }

            List<ZerAppListAdapter.Entry> entries = new ArrayList<>();
            for (UserHandle user : users) {
                List<LauncherActivityInfo> infos;
                try {
                    infos = launcherApps.getActivityList(null, user);
                } catch (Exception e) {
                    continue;
                }
                for (LauncherActivityInfo info : infos) {
                    try {
                        ZerAppListAdapter.Entry entry = new ZerAppListAdapter.Entry();
                        CharSequence label = info.getLabel();
                        if (label == null || label.length() == 0) {
                            label = info.getComponentName().getPackageName();
                        }
                        entry.label = label;
                        entry.component = info.getComponentName();
                        entry.user = info.getUser();
                        entry.icon = info.getBadgedIcon(density);
                        entry.letter = letterOf(label);
                        entries.add(entry);
                    } catch (Exception ignored) {
                        // Skip packages with broken resources.
                    }
                }
            }

            final Collator collator = Collator.getInstance();
            Collections.sort(entries, (a, b) -> {
                int result = collator.compare(a.label.toString(), b.label.toString());
                if (result != 0) {
                    return result;
                }
                return a.component.flattenToString().compareTo(b.component.flattenToString());
            });

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                mAllApps.clear();
                mAllApps.addAll(entries);
                mLoaded = true;
                rebuild();
            });
        });
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

    private void rebuild() {
        String query = mQuery.toLowerCase(Locale.getDefault());
        List<ZerAppListAdapter.Entry> filtered = new ArrayList<>();
        for (ZerAppListAdapter.Entry entry : mAllApps) {
            if (query.isEmpty()
                    || entry.label.toString().toLowerCase(Locale.getDefault()).contains(query)) {
                filtered.add(entry);
            }
        }

        List<Object> rows = new ArrayList<>();
        List<String> letters = new ArrayList<>();
        String lastLetter = null;
        for (ZerAppListAdapter.Entry entry : filtered) {
            if (!entry.letter.equals(lastLetter)) {
                rows.add(entry.letter);
                letters.add(entry.letter);
                lastLetter = entry.letter;
            }
            rows.add(entry);
        }

        mAdapter.setRows(rows);
        mLetterBar.setLetters(letters);
        mEmpty.setVisibility(mLoaded && rows.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void jumpToLetter(String letter) {
        for (int i = 0; i < mAdapter.getItemCount(); i++) {
            Object row = mAdapter.getItem(i);
            if (row instanceof String && letter.equals(row)) {
                RecyclerView.LayoutManager manager = mList.getLayoutManager();
                if (manager instanceof LinearLayoutManager) {
                    ((LinearLayoutManager) manager).scrollToPositionWithOffset(i, 0);
                }
                return;
            }
        }
    }

    private void launch(ZerAppListAdapter.Entry entry) {
        try {
            ActivityOptions options = ActivityOptions.makeCustomAnimation(this,
                    android.R.anim.fade_in, android.R.anim.fade_out);
            mLauncherApps.startMainActivity(entry.component, entry.user, null,
                    options.toBundle());
            finish();
        } catch (Exception e) {
            Toast.makeText(this, entry.label, Toast.LENGTH_SHORT).show();
        }
    }
}
