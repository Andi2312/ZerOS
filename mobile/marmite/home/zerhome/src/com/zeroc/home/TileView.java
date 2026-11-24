package com.zeroc.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;

/** A single flat tile: colour, icon stretched over the whole tile, label. */
public class TileView extends View {

    private final Paint mBackground = new Paint();
    private final Paint mScrim = new Paint();
    private final TextPaint mLabelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TileInfo mInfo;
    private AppEntry mApp;

    public TileView(Context context, TileInfo info) {
        super(context);
        mInfo = info;
        mLabelPaint.setColor(0xFFFFFFFF);
        mLabelPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        mLabelPaint.setTextSize(dp(14));
        mLabelPaint.setShadowLayer(dp(3), 0, dp(1), 0xCC000000);
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
        // Fill the whole tile, keeping the icon's proportions (centre crop).
        int iconWidth = icon.getIntrinsicWidth();
        int iconHeight = icon.getIntrinsicHeight();
        if (iconWidth > 0 && iconHeight > 0) {
            float scale = Math.max((float) width / iconWidth, (float) height / iconHeight);
            int drawWidth = Math.round(iconWidth * scale);
            int drawHeight = Math.round(iconHeight * scale);
            int left = (width - drawWidth) / 2;
            int top = (height - drawHeight) / 2;
            icon.setBounds(left, top, left + drawWidth, top + drawHeight);
        } else {
            icon.setBounds(0, 0, width, height);
        }
        icon.draw(canvas);

        if (mInfo.spanX == 1 && mInfo.spanY == 1) {
            return;
        }

        // Darken the bottom edge so the label stays readable on any icon.
        mScrim.setShader(new LinearGradient(0, height * 0.55f, 0, height,
                0x00000000, 0xB3000000, Shader.TileMode.CLAMP));
        canvas.drawRect(0, height * 0.55f, width, height, mScrim);

        int padding = dp(10);
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
