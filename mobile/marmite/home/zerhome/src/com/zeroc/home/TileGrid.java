package com.zeroc.home;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 4x6 grid of flat tiles with long-press drag, resize and removal.
 */
public class TileGrid extends ViewGroup {

    public interface Listener {
        void onLaunch(TileInfo info);

        void onAppInfo(ComponentName component);

        void onTilesChanged();
    }

    private static final int COLUMNS = 4;
    private static final int ROWS = 6;

    private final int mGap;
    private final int mTouchSlop;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final List<TileView> mTiles = new ArrayList<>();
    private final Map<String, AppEntry> mApps = new HashMap<>();

    private Listener mListener;
    private int mCell;
    private int mLeft;

    private TileView mTouchTile;
    private boolean mDragging;
    private float mDownRawX;
    private float mDownRawY;
    private float mStartX;
    private float mStartY;

    private final Runnable mLongPress = this::startDrag;

    public TileGrid(Context context) {
        super(context);
        mGap = dp(4);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setClipChildren(false);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void setTiles(List<TileInfo> tiles, Map<String, AppEntry> apps) {
        removeAllViews();
        mTiles.clear();
        mApps.clear();
        mApps.putAll(apps);
        for (TileInfo info : tiles) {
            TileView tile = new TileView(getContext(), info);
            tile.setApp(mApps.get(info.key()));
            tile.setOnTouchListener(this::onTileTouch);
            addView(tile);
            mTiles.add(tile);
        }
        requestLayout();
        invalidate();
    }

    public void setApps(Map<String, AppEntry> apps) {
        mApps.clear();
        mApps.putAll(apps);
        for (TileView tile : mTiles) {
            tile.setApp(mApps.get(tile.getInfo().key()));
        }
    }

    public List<TileInfo> getTiles() {
        List<TileInfo> result = new ArrayList<>(mTiles.size());
        for (TileView tile : mTiles) {
            result.add(tile.getInfo());
        }
        return result;
    }

    public boolean isEmpty() {
        return mTiles.isEmpty();
    }

    public boolean isDragging() {
        return mDragging;
    }

    /** Tiles fall back in from the left, one column after another. */
    public void animateTilesIn() {
        for (int i = 0; i < mTiles.size(); i++) {
            TileView tile = mTiles.get(i);
            TileInfo info = tile.getInfo();
            tile.animate().cancel();
            tile.setTranslationX(-dp(18 + info.col * 16));
            tile.setAlpha(0f);
            tile.animate().translationX(0f).alpha(1f)
                    .setStartDelay(i * 16L)
                    .setDuration(240)
                    .setInterpolator(new DecelerateInterpolator(1.5f))
                    .start();
        }
    }

    /** Stops the entrance animation and puts every tile back in place. */
    public void finishTileAnimations() {
        for (TileView tile : mTiles) {
            tile.animate().cancel();
            tile.setAlpha(1f);
            tile.setTranslationX(0f);
        }
    }

    public boolean addApp(AppEntry app) {
        int[] cell = findFreeCell(1, 1);
        if (cell == null) {
            return false;
        }
        TileInfo info = new TileInfo();
        info.component = app.component;
        info.col = cell[0];
        info.row = cell[1];
        info.colorIndex = Palette.forKey(app.key());

        TileView tile = new TileView(getContext(), info);
        tile.setApp(app);
        tile.setOnTouchListener(this::onTileTouch);
        addView(tile);
        mTiles.add(tile);
        requestLayout();
        notifyChanged();
        return true;
    }

    public void cancelDrag() {
        mHandler.removeCallbacks(mLongPress);
        if (mTouchTile != null) {
            resetTile(mTouchTile);
        }
        mTouchTile = null;
        mDragging = false;
    }

    private int[] findFreeCell(int spanX, int spanY) {
        for (int row = 0; row + spanY <= ROWS; row++) {
            for (int col = 0; col + spanX <= COLUMNS; col++) {
                if (canPlace(null, col, row, spanX, spanY)) {
                    return new int[]{col, row};
                }
            }
        }
        return null;
    }

    private boolean canPlace(TileInfo ignore, int col, int row, int spanX, int spanY) {
        if (col < 0 || row < 0 || col + spanX > COLUMNS || row + spanY > ROWS) {
            return false;
        }
        for (TileView tile : mTiles) {
            TileInfo other = tile.getInfo();
            if (other == ignore) {
                continue;
            }
            boolean apart = col + spanX <= other.col
                    || col >= other.col + other.spanX
                    || row + spanY <= other.row
                    || row >= other.row + other.spanY;
            if (!apart) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        if (width <= 0) {
            width = getResources().getDisplayMetrics().widthPixels;
        }
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int byWidth = width / COLUMNS;
        int byHeight = height > 0 ? height / ROWS : byWidth;
        mCell = Math.max(1, Math.min(byWidth, byHeight));
        mLeft = (width - mCell * COLUMNS) / 2;
        for (TileView tile : mTiles) {
            TileInfo info = tile.getInfo();
            int w = Math.max(1, info.spanX * mCell - mGap);
            int h = Math.max(1, info.spanY * mCell - mGap);
            tile.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
        }
        setMeasuredDimension(width, mCell * ROWS);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        for (TileView tile : mTiles) {
            TileInfo info = tile.getInfo();
            int x = mLeft + info.col * mCell + mGap / 2;
            int y = info.row * mCell + mGap / 2;
            int w = info.spanX * mCell - mGap;
            int h = info.spanY * mCell - mGap;
            tile.layout(x, y, x + w, y + h);
        }
    }

    private boolean onTileTouch(View view, MotionEvent event) {
        TileView tile = (TileView) view;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mTouchTile = tile;
                mDragging = false;
                mDownRawX = event.getRawX();
                mDownRawY = event.getRawY();
                mStartX = tile.getX();
                mStartY = tile.getY();
                tile.setAlpha(0.9f);
                mHandler.postDelayed(mLongPress, 450);
                return true;

            case MotionEvent.ACTION_MOVE: {
                float dx = event.getRawX() - mDownRawX;
                float dy = event.getRawY() - mDownRawY;
                if (!mDragging) {
                    if (Math.hypot(dx, dy) > mTouchSlop) {
                        mHandler.removeCallbacks(mLongPress);
                    }
                } else {
                    float x = mStartX + dx;
                    float y = mStartY + dy;
                    x = Math.max(0, Math.min(getWidth() - tile.getWidth(), x));
                    y = Math.max(0, Math.min(getHeight() - tile.getHeight(), y));
                    tile.setX(x);
                    tile.setY(y);
                }
                return true;
            }

            case MotionEvent.ACTION_UP: {
                mHandler.removeCallbacks(mLongPress);
                float dx = event.getRawX() - mDownRawX;
                float dy = event.getRawY() - mDownRawY;
                boolean moved = Math.hypot(dx, dy) > mTouchSlop;
                if (mDragging) {
                    if (moved) {
                        finishDrag(tile);
                    } else {
                        cancelDrag();
                        showMenu(tile);
                    }
                } else if (moved) {
                    resetTile(tile);
                } else {
                    resetTile(tile);
                    if (mListener != null) {
                        mListener.onLaunch(tile.getInfo());
                    }
                }
                mTouchTile = null;
                mDragging = false;
                return true;
            }

            case MotionEvent.ACTION_CANCEL:
                cancelDrag();
                return true;

            default:
                return false;
        }
    }

    private void startDrag() {
        if (mTouchTile == null || mDragging) {
            return;
        }
        mDragging = true;
        mTouchTile.bringToFront();
        mTouchTile.setScaleX(1.05f);
        mTouchTile.setScaleY(1.05f);
        mTouchTile.setElevation(dp(10));
        mTouchTile.setAlpha(0.96f);
        mTouchTile.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
    }

    private void finishDrag(TileView tile) {
        TileInfo info = tile.getInfo();
        int col = Math.round((tile.getX() - mLeft - mGap / 2f) / mCell);
        int row = Math.round((tile.getY() - mGap / 2f) / mCell);
        col = Math.max(0, Math.min(COLUMNS - info.spanX, col));
        row = Math.max(0, Math.min(ROWS - info.spanY, row));
        if (canPlace(info, col, row, info.spanX, info.spanY)) {
            boolean changed = info.col != col || info.row != row;
            info.col = col;
            info.row = row;
            if (changed) {
                notifyChanged();
            }
        }
        resetTile(tile);
        requestLayout();
    }

    private void resetTile(TileView tile) {
        tile.setScaleX(1f);
        tile.setScaleY(1f);
        tile.setElevation(0);
        tile.setAlpha(1f);
        tile.setTranslationX(0);
        tile.setTranslationY(0);
    }

    private void showMenu(final TileView tile) {
        final TileInfo info = tile.getInfo();
        String[] items = {
                getContext().getString(R.string.tile_size),
                getContext().getString(R.string.tile_color),
                getContext().getString(R.string.tile_remove),
                getContext().getString(R.string.app_info)
        };
        new AlertDialog.Builder(getContext())
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showResizeDialog(info);
                            break;
                        case 1:
                            info.colorIndex = Math.floorMod(info.colorIndex + 1, Palette.count());
                            tile.invalidate();
                            notifyChanged();
                            break;
                        case 2:
                            mTiles.remove(tile);
                            removeView(tile);
                            notifyChanged();
                            requestLayout();
                            break;
                        default:
                            if (mListener != null) {
                                mListener.onAppInfo(info.component);
                            }
                            break;
                    }
                })
                .show();
    }

    private void showResizeDialog(final TileInfo info) {
        String[] sizes = {
                getContext().getString(R.string.size_small),
                getContext().getString(R.string.size_medium),
                getContext().getString(R.string.size_wide)
        };
        final int[][] spans = {{1, 1}, {2, 2}, {4, 2}};
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.tile_size)
                .setItems(sizes, (dialog, which) ->
                        resize(info, spans[which][0], spans[which][1]))
                .show();
    }

    private void resize(TileInfo info, int spanX, int spanY) {
        int col = Math.min(info.col, COLUMNS - spanX);
        int row = Math.min(info.row, ROWS - spanY);
        if (!canPlace(info, col, row, spanX, spanY)) {
            Toast.makeText(getContext(), R.string.no_space, Toast.LENGTH_SHORT).show();
            return;
        }
        info.col = col;
        info.row = row;
        info.spanX = spanX;
        info.spanY = spanY;
        notifyChanged();
        requestLayout();
    }

    private void notifyChanged() {
        if (mListener != null) {
            mListener.onTilesChanged();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHandler.removeCallbacks(mLongPress);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
