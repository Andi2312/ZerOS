package dev.zeroc.home;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

/**
 * Две страницы, которые листаются пальцем: рабочий стол и приложения.
 *
 * Обе страницы лежат друг за другом по горизонтали и сдвигаются вместе с
 * пальцем, на отпускании доводятся анимацией. Вертикальные жесты отдаются
 * содержимому (прокрутке стола и сетке приложений).
 */
class PagesView extends FrameLayout {

    interface OnPageChanged {
        void onPageChanged(int page);
    }

    static final int HOME = 0;
    static final int APPS = 1;

    private View home;
    private View apps;
    private int page = HOME;

    private float downX;
    private float downY;
    private float grabX;
    private boolean dragging;
    private final int slop;

    private OnPageChanged listener;

    PagesView(Context context) {
        super(context);
        slop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    void setPages(View homePage, View appsPage) {
        home = homePage;
        apps = appsPage;
        addView(home, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(apps, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        home.setTranslationX(0f);
        apps.setTranslationX(page == APPS ? 0f : width());
    }

    void setOnPageChangedListener(OnPageChanged callback) {
        listener = callback;
    }

    int page() {
        return page;
    }

    void open(int target, boolean animate) {
        page = target;
        int w = width();
        float homeX = target == APPS ? -w : 0f;
        float appsX = target == APPS ? 0f : w;

        if (w == 0 || !animate) {
            home.setTranslationX(homeX);
            apps.setTranslationX(appsX);
        } else {
            home.animate()
                    .translationX(homeX)
                    .setDuration(240)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
            apps.animate()
                    .translationX(appsX)
                    .setDuration(240)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        if (listener != null) {
            listener.onPageChanged(page);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (home == null || apps == null) {
            return;
        }
        home.setTranslationX(page == APPS ? -w : 0f);
        apps.setTranslationX(page == APPS ? 0f : w);
    }

    private int width() {
        return getWidth();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (home == null || apps == null) {
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                dragging = false;
                break;
            case MotionEvent.ACTION_MOVE: {
                float dx = event.getX() - downX;
                float dy = event.getY() - downY;
                if (!dragging && Math.abs(dx) > slop && Math.abs(dx) > Math.abs(dy) * 1.2f) {
                    dragging = true;
                    grabX = apps.getTranslationX();
                    if (getParent() != null) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    return true;
                }
                break;
            }
            default:
                break;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (home == null || apps == null) {
            return false;
        }
        int w = Math.max(1, width());

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_MOVE: {
                float dx = event.getX() - downX;
                float x = Math.max(-w, Math.min(w, grabX + dx));
                apps.setTranslationX(x);
                home.setTranslationX(x - w);
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                boolean toApps = apps.getTranslationX() < w * 0.5f;
                open(toApps ? APPS : HOME, true);
                dragging = false;
                return true;
            }
            default:
                return true;
        }
    }
}
