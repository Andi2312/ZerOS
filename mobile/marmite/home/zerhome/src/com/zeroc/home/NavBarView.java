package com.zeroc.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

/** Black bar: Back, Start and the app list button in the right corner. */
public class NavBarView extends View {

    public interface Listener {
        void onBack();

        void onHome();

        void onApps();
    }

    private static final int ZONE_NONE = -1;
    private static final int ZONE_BACK = 0;
    private static final int ZONE_HOME = 1;
    private static final int ZONE_APPS = 2;

    private final Paint mBackground = new Paint();
    private final Paint mPress = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mPath = new Path();

    private Listener mListener;
    private int mPressed = ZONE_NONE;

    public NavBarView(Context context) {
        super(context);
        mBackground.setColor(0xFF000000);
        mPress.setColor(0x1FFFFFFF);
        mStroke.setColor(0xFFFFFFFF);
        mStroke.setStyle(Paint.Style.STROKE);
        mStroke.setStrokeWidth(dp(1.4f));
        mStroke.setStrokeCap(Paint.Cap.ROUND);
        mStroke.setStrokeJoin(Paint.Join.ROUND);
        mFill.setColor(0xFFFFFFFF);
        mFill.setStyle(Paint.Style.FILL);
        setClickable(true);
        setContentDescription(getContext().getString(R.string.nav_bar));
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        canvas.drawRect(0, 0, width, height, mBackground);

        float cy = height / 2f;
        float backX = dp(34);
        float homeX = width / 2f;
        float appsX = width - dp(30);

        if (mPressed != ZONE_NONE) {
            float cx = mPressed == ZONE_BACK ? backX : mPressed == ZONE_HOME ? homeX : appsX;
            canvas.drawCircle(cx, cy, dp(19), mPress);
        }

        drawBack(canvas, backX, cy);
        drawHome(canvas, homeX, cy);
        drawApps(canvas, appsX, cy);
    }

    private void drawBack(Canvas canvas, float cx, float cy) {
        canvas.drawCircle(cx, cy, dp(9), mStroke);
        mPath.reset();
        mPath.moveTo(cx + dp(3.2f), cy);
        mPath.lineTo(cx - dp(3.2f), cy);
        mPath.moveTo(cx - dp(0.6f), cy - dp(2.6f));
        mPath.lineTo(cx - dp(3.2f), cy);
        mPath.lineTo(cx - dp(0.6f), cy + dp(2.6f));
        canvas.drawPath(mPath, mStroke);
    }

    private void drawHome(Canvas canvas, float cx, float cy) {
        float size = dp(4.6f);
        float gap = dp(1.4f);
        float left = cx - (size + gap / 2f);
        float top = cy - (size + gap / 2f);
        canvas.drawRect(left, top, left + size, top + size, mFill);
        canvas.drawRect(left + size + gap, top, left + 2 * size + gap, top + size, mFill);
        canvas.drawRect(left, top + size + gap, left + size, top + 2 * size + gap, mFill);
        canvas.drawRect(left + size + gap, top + size + gap,
                left + 2 * size + gap, top + 2 * size + gap, mFill);
    }

    /** Three list rows: the app list lives behind this button. */
    private void drawApps(Canvas canvas, float cx, float cy) {
        float half = dp(4.8f);
        float step = dp(4.2f);
        float stroke = dp(1.6f);
        mStroke.setStrokeWidth(stroke);
        for (int i = -1; i <= 1; i++) {
            float y = cy + i * step;
            canvas.drawLine(cx - half, y, cx + half, y, mStroke);
        }
        mStroke.setStrokeWidth(dp(1.4f));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mPressed = zoneAt(event.getX());
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE: {
                int zone = zoneAt(event.getX());
                if (zone != mPressed) {
                    mPressed = zone;
                    invalidate();
                }
                return true;
            }
            case MotionEvent.ACTION_UP: {
                int zone = zoneAt(event.getX());
                if (mPressed != ZONE_NONE) {
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    fire(zone);
                }
                mPressed = ZONE_NONE;
                invalidate();
                performClick();
                return true;
            }
            case MotionEvent.ACTION_CANCEL:
                mPressed = ZONE_NONE;
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

    private int zoneAt(float x) {
        int width = getWidth();
        if (width <= 0) {
            return ZONE_NONE;
        }
        if (x < width * 0.34f) {
            return ZONE_BACK;
        }
        if (x < width * 0.72f) {
            return ZONE_HOME;
        }
        return ZONE_APPS;
    }

    private void fire(int zone) {
        if (mListener == null) {
            return;
        }
        if (zone == ZONE_BACK) {
            mListener.onBack();
        } else if (zone == ZONE_HOME) {
            mListener.onHome();
        } else {
            mListener.onApps();
        }
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
