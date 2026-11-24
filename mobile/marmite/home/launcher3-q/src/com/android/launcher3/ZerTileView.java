/*
 * ZerOS: Windows Phone style workspace tile.
 *
 * The stock BubbleTextView draws a centred icon above a centred label.  A tile
 * fills its whole grid cell with a flat marsala rectangle, keeps the icon in
 * the top left corner and the label in the bottom left corner, like the live
 * tiles of Windows Phone.
 *
 * Only items that live directly on the workspace are drawn this way: folder
 * children and hotseat icons keep the stock look, so the class is safe to use
 * as the root of app_icon.xml.
 */
package com.android.launcher3;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;

public class ZerTileView extends BubbleTextView {

    private static final int COLOR_TILE = 0xFF4A0A18;
    private static final int COLOR_TILE_PRESSED = 0xFF5C0D1E;
    private static final int COLOR_TEXT = 0xFFF2A6A0;

    private final Paint mTilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint mLabelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Rect mIconBounds = new Rect();
    private final Rect mTileBounds = new Rect();

    /** Gap between neighbouring tiles. */
    private final int mGapPx;
    /** Padding between the tile border and its content. */
    private final int mPaddingPx;
    /** Gap between the icon and the label. */
    private final int mTextGapPx;

    public ZerTileView(Context context) {
        this(context, null, 0);
    }

    public ZerTileView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZerTileView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        float density = getResources().getDisplayMetrics().density;
        mGapPx = Math.round(2.5f * density);
        mPaddingPx = Math.round(5f * density);
        mTextGapPx = Math.round(2f * density);
    }

    private boolean isTile() {
        Object tag = getTag();
        return tag instanceof ItemInfo
                && ((ItemInfo) tag).container == LauncherSettings.Favorites.CONTAINER_DESKTOP;
    }

    private boolean isPressedState() {
        for (int state : getDrawableState()) {
            if (state == android.R.attr.state_pressed) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (!isTile()) {
            super.onDraw(canvas);
            return;
        }

        int width = getWidth();
        int height = getHeight();
        int left = mGapPx;
        int top = mGapPx;
        int right = width - mGapPx;
        int bottom = height - mGapPx;
        if (right <= left || bottom <= top) {
            return;
        }

        mTileBounds.set(left, top, right, bottom);
        mTilePaint.setColor(isPressedState() ? COLOR_TILE_PRESSED : COLOR_TILE);
        canvas.drawRect(mTileBounds, mTilePaint);

        int contentLeft = left + mPaddingPx;
        int contentTop = top + mPaddingPx;
        int contentRight = right - mPaddingPx;
        int contentBottom = bottom - mPaddingPx;
        int contentWidth = contentRight - contentLeft;
        int contentHeight = contentBottom - contentTop;
        if (contentWidth <= 0 || contentHeight <= 0) {
            return;
        }

        mLabelPaint.set(getPaint());
        mLabelPaint.setColor(COLOR_TEXT);
        mLabelPaint.setAlpha(Math.round(255f * getTextAlphaValue()));
        Paint.FontMetrics fm = mLabelPaint.getFontMetrics();
        int labelHeight = (int) Math.ceil(fm.descent - fm.ascent);
        int iconSpace = contentHeight - labelHeight - mTextGapPx;

        ItemInfo info = (ItemInfo) getTag();
        int wantedIconSize = getIconSize();
        if (info.spanX >= 2 && info.spanY >= 2) {
            // Large tiles get a slightly larger icon, like a Windows Phone
            // medium/live tile.
            wantedIconSize = Math.round(wantedIconSize * 1.3f);
        }
        int iconSize = Math.min(wantedIconSize, Math.min(contentWidth, iconSpace));
        Drawable icon = getIcon();
        if (isIconVisible() && icon != null && iconSize > 0) {
            mIconBounds.set(contentLeft, contentTop, contentLeft + iconSize, contentTop + iconSize);
            icon.setBounds(mIconBounds);
            icon.draw(canvas);
        }

        CharSequence text = getText();
        if (!TextUtils.isEmpty(text)) {
            CharSequence line = TextUtils.ellipsize(text, mLabelPaint, contentWidth,
                    TextUtils.TruncateAt.END);
            float baseline = contentBottom - fm.descent;
            canvas.drawText(line, 0, line.length(), contentLeft, baseline, mLabelPaint);
        }
    }
}
