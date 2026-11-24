/*
 * ZerOS: alphabetical index on the right edge of the app list.
 */
package com.android.launcher3.zer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.android.launcher3.R;

import java.util.ArrayList;
import java.util.List;

public class ZerLetterBar extends View {

    public interface Listener {
        void onLetterSelected(String letter);
    }

    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mActivePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<String> mLetters = new ArrayList<>();

    private Listener mListener;
    private int mActive = -1;
    private float mItemHeight;

    public ZerLetterBar(Context context) {
        this(context, null);
    }

    public ZerLetterBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        float size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10,
                getResources().getDisplayMetrics());
        mTextPaint.setTextSize(size);
        mTextPaint.setColor(0x66FFFFFF);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mActivePaint.setTextSize(size);
        mActivePaint.setColor(0xFFFFFFFF);
        mActivePaint.setTextAlign(Paint.Align.CENTER);
        mActivePaint.setFakeBoldText(true);
        setClickable(true);
        setContentDescription(getContext().getString(R.string.zer_alphabet));
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void setLetters(List<String> letters) {
        mLetters.clear();
        mLetters.addAll(letters);
        mActive = -1;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mLetters.isEmpty()) {
            return;
        }
        mItemHeight = getHeight() / (float) mLetters.size();
        float centerX = getWidth() / 2f;
        for (int i = 0; i < mLetters.size(); i++) {
            Paint paint = i == mActive ? mActivePaint : mTextPaint;
            Paint.FontMetrics fm = paint.getFontMetrics();
            float baseline = i * mItemHeight + mItemHeight / 2f
                    - (fm.ascent + fm.descent) / 2f;
            canvas.drawText(mLetters.get(i), centerX, baseline, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mLetters.isEmpty()) {
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE: {
                if (mItemHeight <= 0) {
                    mItemHeight = getHeight() / (float) mLetters.size();
                }
                int index = (int) (event.getY() / mItemHeight);
                index = Math.max(0, Math.min(mLetters.size() - 1, index));
                if (index != mActive) {
                    mActive = index;
                    invalidate();
                    if (mListener != null) {
                        mListener.onLetterSelected(mLetters.get(index));
                    }
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
                performClick();
                mActive = -1;
                invalidate();
                return true;
            case MotionEvent.ACTION_CANCEL:
                mActive = -1;
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
}
