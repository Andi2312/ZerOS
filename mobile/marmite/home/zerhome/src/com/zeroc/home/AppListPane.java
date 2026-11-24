package com.zeroc.home;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Full screen alphabetical app list over the Start screen. */
public class AppListPane extends FrameLayout {

    public interface Listener {
        void onLaunch(AppEntry entry);

        void onPin(AppEntry entry);

        void onAppInfo(AppEntry entry);
    }

    private final List<AppEntry> mAllApps = new ArrayList<>();
    private final AppListAdapter mAdapter;
    private final ListView mList;
    private final LetterBar mLetterBar;
    private final FrameLayout mSearchBox;
    private final EditText mSearch;
    private final TextView mEmpty;

    private Listener mListener;
    private String mQuery = "";
    private boolean mClosing;

    public AppListPane(Context context) {
        super(context);
        setBackgroundColor(0xFF000000);
        setVisibility(GONE);

        mAdapter = new AppListAdapter(context, new AppListAdapter.Listener() {
            @Override
            public void onAppClick(AppEntry entry) {
                if (mListener != null) {
                    mListener.onLaunch(entry);
                }
            }

            @Override
            public void onAppLongClick(AppEntry entry) {
                showAppMenu(entry);
            }
        });

        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);

        mSearchBox = new FrameLayout(context);
        mSearchBox.setBackgroundColor(0xFF0A0A0A);
        mSearchBox.setVisibility(GONE);
        mSearch = new EditText(context);
        mSearch.setBackground(null);
        mSearch.setSingleLine(true);
        mSearch.setTextColor(0xFFFFFFFF);
        mSearch.setHintTextColor(0x99FFFFFF);
        mSearch.setHint(R.string.search_hint);
        mSearch.setTextSize(17);
        mSearch.setPadding(dp(20), 0, dp(20), 0);
        mSearch.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        mSearch.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
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
        mSearchBox.addView(mSearch, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        column.addView(mSearchBox, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        FrameLayout listArea = new FrameLayout(context);

        mList = new ListView(context);
        mList.setDivider(null);
        mList.setDividerHeight(0);
        mList.setCacheColorHint(0);
        mList.setClipToPadding(false);
        mList.setPadding(0, 0, dp(28), 0);
        mList.setAdapter(mAdapter);
        listArea.addView(mList, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mLetterBar = new LetterBar(context);
        FrameLayout.LayoutParams letterParams = new FrameLayout.LayoutParams(
                dp(28), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.END);
        listArea.addView(mLetterBar, letterParams);
        mLetterBar.setListener(this::jumpToLetter);

        mEmpty = new TextView(context);
        mEmpty.setText(R.string.nothing_found);
        mEmpty.setTextColor(0x99FFFFFF);
        mEmpty.setTextSize(17);
        mEmpty.setVisibility(GONE);
        listArea.addView(mEmpty, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));

        column.addView(listArea, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        addView(column, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void setApps(List<AppEntry> apps) {
        mAllApps.clear();
        mAllApps.addAll(apps);
        rebuild();
    }

    public boolean isOpen() {
        return getVisibility() == VISIBLE;
    }

    public void open(boolean withSearch) {
        if (isOpen() && !mClosing) {
            if (withSearch) {
                showSearch();
            }
            return;
        }
        mClosing = false;
        animate().cancel();
        setVisibility(VISIBLE);
        setAlpha(0f);
        setTranslationY(dp(28));
        animate().alpha(1f).translationY(0f).setDuration(200).start();
        if (withSearch) {
            post(this::showSearch);
        }
    }

    public void close() {
        if (!isOpen() || mClosing) {
            return;
        }
        hideSearchInternal();
        mClosing = true;
        animate().cancel();
        animate().alpha(0f).translationY(dp(20)).setDuration(160)
                .withEndAction(() -> {
                    if (mClosing) {
                        setVisibility(GONE);
                        mClosing = false;
                    }
                })
                .start();
    }

    public boolean isSearchOpen() {
        return mSearchBox.getVisibility() == VISIBLE;
    }

    /** Returns true when the search field was open and is now closed. */
    public boolean hideSearch() {
        if (!isSearchOpen()) {
            return false;
        }
        hideSearchInternal();
        return true;
    }

    private void showSearch() {
        mSearchBox.setVisibility(VISIBLE);
        mSearch.requestFocus();
        mSearch.post(() -> {
            InputMethodManager imm = (InputMethodManager)
                    getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(mSearch, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    private void hideSearchInternal() {
        if (isSearchOpen()) {
            mSearch.setText("");
            mSearchBox.setVisibility(GONE);
            InputMethodManager imm = (InputMethodManager)
                    getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(mSearch.getWindowToken(), 0);
            }
        }
    }

    private void rebuild() {
        String query = mQuery.toLowerCase(Locale.getDefault());
        List<Object> rows = new ArrayList<>();
        List<String> letters = new ArrayList<>();
        String lastLetter = null;
        for (AppEntry entry : mAllApps) {
            if (!query.isEmpty()
                    && !entry.label.toString().toLowerCase(Locale.getDefault()).contains(query)) {
                continue;
            }
            if (!entry.letter.equals(lastLetter)) {
                rows.add(entry.letter);
                letters.add(entry.letter);
                lastLetter = entry.letter;
            }
            rows.add(entry);
        }
        mAdapter.setRows(rows);
        mLetterBar.setLetters(letters);
        boolean empty = !mAllApps.isEmpty() && rows.isEmpty();
        mEmpty.setVisibility(empty ? VISIBLE : GONE);
    }

    /** Debug/automation hook: scroll to an alphabet section. */
    public void jumpTo(String letter) {
        jumpToLetter(letter);
    }

    private void jumpToLetter(String letter) {
        for (int i = 0; i < mAdapter.getCount(); i++) {
            Object row = mAdapter.getItem(i);
            if (row instanceof String && letter.equals(row)) {
                mList.setSelectionFromTop(i, 0);
                return;
            }
        }
    }

    private void showAppMenu(final AppEntry entry) {
        String[] items = {
                getContext().getString(R.string.pin_to_start),
                getContext().getString(R.string.app_info)
        };
        new AlertDialog.Builder(getContext())
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        if (mListener != null) {
                            mListener.onPin(entry);
                        }
                    } else if (mListener != null) {
                        mListener.onAppInfo(entry);
                    }
                })
                .show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
