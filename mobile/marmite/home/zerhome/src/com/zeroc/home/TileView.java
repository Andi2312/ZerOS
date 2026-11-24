package com.zeroc.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;

/** A single flat tile: colour, icon, label. */
public class TileView extends View {

    private final Paint mBackground = new Paint();
    private final TextPaint mLabelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TileInfo mInfo;
    private AppEntry mApp;

    public TileView(Context context, TileInfo info) {
        super(context);
        mInfo = info;
        mLabelPaint.setColor(0xFFFFFFFF);
        mLabelPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        mLabelPaint.setTextSize(dp(14));
    }

    public TileInfo getInfo() {
        return mInfo;
    }

    public void setApp(AppEntry app) {
        mApp = app;
        if (app != null) {
            setContentDescription(app.label);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        mBackground.setColor(Palette.color(mInfo.colorIndex));
        canvas.drawRect(0, 0, width, height, mBackground);

        if (mApp == null || mApp.icon == null) {
            return;
        }
        Drawable icon = mApp.icon;

        if (mInfo.spanX == 1 && mInfo.spanY == 1) {
            int size = Math.round(Math.min(width, height) * 0.5f);
            int left = (width - size) / 2;
            int top = (height - size) / 2;
            icon.setBounds(left, top, left + size, top + size);
            icon.draw(canvas);
            return;
        }

        int padding = dp(12);
        int iconSize = dp(mInfo.spanX >= 4 ? 44 : 40);
        icon.setBounds(padding, padding, padding + iconSize, padding + iconSize);
        icon.draw(canvas);

        int maxWidth = width - padding * 2;
        CharSequence label = TextUtils.ellipsize(
                mApp.label, mLabelPaint, maxWidth, TextUtils.TruncateAt.END);
        float baseline = height - padding - mLabelPaint.descent();
        canvas.drawText(label.toString(), padding, baseline, mLabelPaint);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
