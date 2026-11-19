package dev.zeroc.home;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import java.util.List;

/**
 * Горизонтальный пейджер страниц рабочего стола без androidx.
 *
 * Внутри HorizontalScrollView лежит полоса страниц шириной в экран; после
 * отпускания палец «прилипает» к ближайшей странице, флинг перекидывает ровно
 * на одну. Так страницы листаются на API 22+, где ViewPager из framework нет.
 */
class DesktopPager extends HorizontalScrollView {

    interface Listener {
        void onPageChanged(int page, int total);
    }

    private final LinearLayout strip;
    private int pageWidth;
    private int currentPage;
    private int totalPages;
    private boolean flinged;
    private Listener listener;

    DesktopPager(Context context) {
        super(context);
        setHorizontalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
        setFillViewport(true);
        pageWidth = context.getResources().getDisplayMetrics().widthPixels;

        strip = new LinearLayout(context);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        addView(strip, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    void setPages(List<View> pages) {
        strip.removeAllViews();
        totalPages = pages.size();

        for (int i = 0; i < pages.size(); i++) {
            View page = pages.get(i);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    pageWidth, LayoutParams.MATCH_PARENT);
            strip.addView(page, lp);
        }

        if (totalPages == 0) {
            currentPage = 0;
            return;
        }
        if (currentPage >= totalPages) {
            currentPage = totalPages - 1;
        }
        final int target = currentPage;
        post(new Runnable() {
            @Override
            public void run() {
                scrollTo(target * pageWidth, 0);
                notifyPage();
            }
        });
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || w == pageWidth) {
            return;
        }
        pageWidth = w;
        for (int i = 0; i < strip.getChildCount(); i++) {
            View child = strip.getChildAt(i);
            child.getLayoutParams().width = pageWidth;
            child.requestLayout();
        }
        scrollTo(currentPage * pageWidth, 0);
        notifyPage();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = super.onTouchEvent(event);
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (!flinged) {
                snapToNearest();
            }
            flinged = false;
        }
        return handled;
    }

    @Override
    public void fling(int velocityX) {
        if (pageWidth <= 0) {
            return;
        }
        flinged = true;
        int target = pageAt(getScrollX()) + (velocityX > 0 ? 1 : -1);
        goTo(clamp(target));
    }

    private void snapToNearest() {
        if (pageWidth <= 0) {
            return;
        }
        float exact = getScrollX() / (float) pageWidth;
        goTo(clamp(Math.round(exact)));
    }

    private void goTo(final int page) {
        if (page == currentPage && getScrollX() == page * pageWidth) {
            notifyPage();
            return;
        }
        smoothScrollTo(page * pageWidth, 0);
        postDelayed(new Runnable() {
            @Override
            public void run() {
                currentPage = page;
                notifyPage();
            }
        }, 260);
    }

    private int pageAt(int scrollX) {
        if (pageWidth <= 0) {
            return 0;
        }
        return clamp(Math.round(scrollX / (float) pageWidth));
    }

    private int clamp(int page) {
        if (totalPages <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(totalPages - 1, page));
    }

    private void notifyPage() {
        if (listener == null) {
            return;
        }
        int page = pageAt(getScrollX());
        if (getScrollX() == 0 && currentPage == 0) {
            page = 0;
        }
        currentPage = page;
        listener.onPageChanged(currentPage, totalPages);
    }
}
