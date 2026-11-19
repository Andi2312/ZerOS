package dev.zeroc.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/**
 * Фон ZerOS: тёмная база #090A0E и мягкие пятна Zero.C — коралл, тёплый розовый,
 * приглушённый фиолет. Пятна размыты сами по себе (radial gradient), поэтому
 * не требуют blur — работают на любом API и не жрут батарею.
 */
class AuroraView extends View {

    private final Paint basePaint = new Paint();
    private final Paint blobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint[] blobs = new Paint[4];
    private final float[] centers = {0.08f, 0.02f, 0.98f, 0.10f, 0.18f, 1.02f, 0.62f, 0.42f};
    private final int[] colors = {0x3DFF6259, 0x33D6C2C1, 0x2E6F5FA8, 0x14FFFFFF};
    private final float[] sizes = {0.85f, 0.70f, 0.90f, 0.55f};

    private int lastWidth;
    private int lastHeight;

    AuroraView(Context context) {
        super(context);
        for (int i = 0; i < blobs.length; i++) {
            blobs[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
            blobs[i].setColor(colors[i]);
        }
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        build(w, h);
    }

    private void build(int w, int h) {
        if (w <= 0 || h <= 0) {
            return;
        }
        lastWidth = w;
        lastHeight = h;

        basePaint.setShader(new LinearGradient(0, 0, 0, h,
                0xFF090A0E, 0xFF111014, Shader.TileMode.CLAMP));

        for (int i = 0; i < blobs.length; i++) {
            float cx = centers[i * 2] * w;
            float cy = centers[i * 2 + 1] * h;
            float radius = sizes[i] * Math.max(w, h);
            blobs[i].setShader(new RadialGradient(cx, cy, radius,
                    colors[i], colors[i] & 0x00FFFFFF, Shader.TileMode.CLAMP));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w != lastWidth || h != lastHeight) {
            build(w, h);
        }

        canvas.drawRect(0, 0, w, h, basePaint);
        for (Paint blob : blobs) {
            if (blob.getShader() != null) {
                canvas.drawRect(0, 0, w, h, blob);
            }
        }
    }
}
