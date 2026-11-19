package dev.zeroc.home;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** Сетка иконок приложений: стеклянная плитка, ripple, подпись в одну строку (MD3). */
class AppAdapter extends BaseAdapter {

    private static final int TILE = 0x1AFFFFFF;
    private static final int EDGE_ALPHA = 0x3D;
    private static final int LABEL = 0xFFD7DEE5;
    private static final int RIPPLE = 0x24FFFFFF;

    private static final int TILE_SIZE_DP = 60;
    private static final int TILE_RADIUS_DP = 18;
    private static final int ICON_PADDING_DP = 12;

    private final Context ctx;
    private final List<AppEntry> data;
    private final Typeface regular;
    private final Typeface medium;
    private final float density;
    private final Resources res;

    AppAdapter(Context ctx, List<AppEntry> data, Typeface regular, Typeface medium) {
        this.ctx = ctx;
        this.data = data;
        this.regular = regular;
        this.medium = medium;
        this.res = ctx.getResources();
        this.density = res.getDisplayMetrics().density;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return data.get(position).packageName.hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;
        if (convertView == null) {
            LinearLayout item = new LinearLayout(ctx);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER_HORIZONTAL);
            item.setPadding(0, dp(2), 0, dp(2));

            ImageView icon = new ImageView(ctx);
            int size = dp(TILE_SIZE_DP);
            icon.setLayoutParams(new LinearLayout.LayoutParams(size, size));
            int pad = dp(ICON_PADDING_DP);
            icon.setPadding(pad, pad, pad, pad);
            // ripple должен получать pressed-состояние от ячейки GridView
            icon.setDuplicateParentStateEnabled(true);
            icon.setBackground(tileBackground());
            item.addView(icon);

            TextView label = new TextView(ctx);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(7);
            label.setLayoutParams(lp);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            label.setTextColor(LABEL);
            label.setTypeface(medium);
            label.setGravity(Gravity.CENTER_HORIZONTAL);
            label.setSingleLine(true);
            label.setEllipsize(TextUtils.TruncateAt.END);
            label.setIncludeFontPadding(false);
            item.addView(label);

            holder = new Holder(icon, label);
            item.setTag(holder);
            convertView = item;
        } else {
            holder = (Holder) convertView.getTag();
        }

        AppEntry entry = data.get(position);
        holder.label.setText(entry.label);
        holder.icon.setImageDrawable(entry.icon);
        return convertView;
    }

    /** Стеклянная плитка: ripple поверх GlassDrawable, маска по тому же радиусу. */
    private RippleDrawable tileBackground() {
        GlassDrawable fill = new GlassDrawable(TILE, dp(TILE_RADIUS_DP),
                Math.max(1f, dp(0.7f)), EDGE_ALPHA);

        GradientDrawable mask = new GradientDrawable();
        mask.setColor(0xFFFFFFFF);
        mask.setCornerRadius(dp(TILE_RADIUS_DP));

        return new RippleDrawable(ColorStateList.valueOf(RIPPLE), fill, mask);
    }

    private int dp(float value) {
        return Math.round(value * density);
    }

    private static final class Holder {
        final ImageView icon;
        final TextView label;

        Holder(ImageView icon, TextView label) {
            this.icon = icon;
            this.label = label;
        }
    }
}
