package dev.zeroc.home;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * Стеклянная панель: полупрозрачная заливка, светящаяся кромка (ярче сверху)
 * и вертикальный блик. Так «жидкое стекло» выглядит одинаково на API 22+,
 * где настоящего blur в системе ещё нет.
 */
class GlassDrawable extends Drawable {

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sheenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final RectF inner = new RectF();

    private final float radius;
    private final float edgeWidth;
    private final int edgeTopColor;
    private final int edgeBottomColor;
    private final int sheenColor;

    private Shader edgeShader;
    private Shader sheenShader;

    GlassDrawable(int fillColor, float radiusPx, float edgeWidthPx, int edgeAlpha) {
        this.radius = radiusPx;
        this.edgeWidth = Math.max(1f, edgeWidthPx);
        this.edgeTopColor = Color.argb(clamp(edgeAlpha), 255, 255, 255);
        this.edgeBottomColor = Color.argb(clamp(edgeAlpha / 3), 255, 255, 255);
        this.sheenColor = Color.argb(clamp(edgeAlpha / 3), 255, 255, 255);

        fillPaint.setColor(fillColor);
        edgePaint.setStyle(Paint.Style.STROKE);
        edgePaint.setStrokeWidth(this.edgeWidth);
        sheenPaint.setColor(sheenColor);
    }

    private static int clamp(int alpha) {
        return Math.max(0, Math.min(255, alpha));
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        rect.set(bounds);
        rect.inset(edgeWidth / 2f, edgeWidth / 2f);
        inner.set(rect);
        inner.inset(edgeWidth / 2f, edgeWidth / 2f);

        edgeShader = new LinearGradient(0, rect.top, 0, rect.bottom,
                edgeTopColor, edgeBottomColor, Shader.TileMode.CLAMP);
        sheenShader = new LinearGradient(0, inner.top, 0, inner.top + inner.height() * 0.55f,
                sheenColor, Color.TRANSPARENT, Shader.TileMode.CLAMP);
    }

    @Override
    public void draw(Canvas canvas) {
        if (edgeShader == null) {
            onBoundsChange(getBounds());
        }

        canvas.drawRoundRect(inner, radius, radius, fillPaint);

        edgePaint.setShader(edgeShader);
        canvas.drawRoundRect(rect, radius, radius, edgePaint);

        sheenPaint.setShader(sheenShader);
        canvas.drawRoundRect(inner, radius, radius, sheenPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        fillPaint.setAlpha(alpha);
        edgePaint.setAlpha(alpha);
        sheenPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        fillPaint.setColorFilter(colorFilter);
        edgePaint.setColorFilter(colorFilter);
        sheenPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
