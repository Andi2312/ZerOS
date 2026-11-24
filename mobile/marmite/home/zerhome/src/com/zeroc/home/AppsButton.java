package com.zeroc.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

/** Floating round button above the nav bar: opens the app list. */
public class AppsButton extends View {

    private final Paint mCircle = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mLines = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean mPressed;

    public AppsButton(Context context) {
        super(context);
        setClickable(true);
        mLines.setColor(0xFFFFFFFF);
        mLines.setStyle(Paint.Style.STROKE);
        mLines.setStrokeWidth(dp(1.8f));
        mLines.setStrokeCap(Paint.Cap.ROUND);
        setContentDescription(getContext().getString(R.string.open_app_list));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(getWidth(), getHeight()) / 2f - dp(1);

        mCircle.setColor(mPressed ? 0xCC000000 : 0x99000000);
        canvas.drawCircle(cx, cy, radius, mCircle);

        float half = dp(8);
        float step = dp(5);
        for (int i = -1; i <= 1; i++) {
            float y = cy + i * step;
            canvas.drawLine(cx - half, y, cx + half, y, mLines);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mPressed = true;
                animate().scaleX(0.92f).scaleY(0.92f).setDuration(90).start();
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                mPressed = false;
                animate().scaleX(1f).scaleY(1f).setDuration(90).start();
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                performClick();
                invalidate();
                return true;
            case MotionEvent.ACTION_CANCEL:
                mPressed = false;
                animate().scaleX(1f).scaleY(1f).setDuration(90).start();
                invalidate();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
