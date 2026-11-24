package com.zeroc.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

/** Black bar with Back / Start / Search, drawn Windows Phone style. */
public class NavBarView extends View {

    public interface Listener {
        void onBack();

        void onHome();

        void onSearch();
    }

    private static final int ZONE_NONE = -1;
    private static final int ZONE_BACK = 0;
    private static final int ZONE_HOME = 1;
    private static final int ZONE_SEARCH = 2;

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

        float zone = width / 3f;
        float cy = height / 2f;
        if (mPressed != ZONE_NONE) {
            canvas.drawCircle(zone * (mPressed + 0.5f), cy, dp(19), mPress);
        }
        drawBack(canvas, zone * 0.5f, cy);
        drawHome(canvas, zone * 1.5f, cy);
        drawSearch(canvas, zone * 2.5f, cy);
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

    private void drawSearch(Canvas canvas, float cx, float cy) {
        canvas.drawCircle(cx - dp(1), cy - dp(1), dp(6.2f), mStroke);
        mPath.reset();
        mPath.moveTo(cx + dp(3.4f), cy + dp(3.4f));
        mPath.lineTo(cx + dp(7), cy + dp(7));
        canvas.drawPath(mPath, mStroke);
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
        if (getWidth() <= 0) {
            return ZONE_NONE;
        }
        int zone = (int) (x / (getWidth() / 3f));
        return Math.max(ZONE_BACK, Math.min(ZONE_SEARCH, zone));
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
            mListener.onSearch();
        }
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
