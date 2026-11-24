/*
 * ZerOS: adapter for the Windows Phone style app list.
 */
package com.android.launcher3.zer;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.R;

import java.util.ArrayList;
import java.util.List;

public class ZerAppListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onAppClick(Entry entry);
    }

    public static class Entry {
        public CharSequence label;
        public ComponentName component;
        public UserHandle user;
        public Drawable icon;
        public String letter;
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_APP = 1;

    private final LayoutInflater mInflater;
    private final Listener mListener;
    private final List<Object> mRows = new ArrayList<>();

    public ZerAppListAdapter(Context context, Listener listener) {
        mInflater = LayoutInflater.from(context);
        mListener = listener;
    }

    public void setRows(List<Object> rows) {
        mRows.clear();
        mRows.addAll(rows);
        notifyDataSetChanged();
    }

    public Object getItem(int position) {
        return mRows.get(position);
    }

    @Override
    public int getItemCount() {
        return mRows.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mRows.get(position) instanceof Entry ? TYPE_APP : TYPE_HEADER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = mInflater.inflate(R.layout.zer_app_list_header, parent, false);
            return new HeaderHolder(view);
        }
        View view = mInflater.inflate(R.layout.zer_app_list_item, parent, false);
        return new AppHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object row = mRows.get(position);
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).header.setText((String) row);
            return;
        }
        final Entry entry = (Entry) row;
        AppHolder appHolder = (AppHolder) holder;
        appHolder.label.setText(entry.label);
        appHolder.icon.setImageDrawable(entry.icon);
        appHolder.itemView.setContentDescription(entry.label);
        appHolder.itemView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onAppClick(entry);
            }
        });
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        final TextView header;

        HeaderHolder(View view) {
            super(view);
            header = view.findViewById(R.id.zer_header);
        }
    }

    static class AppHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView label;

        AppHolder(View view) {
            super(view);
            icon = view.findViewById(R.id.zer_item_icon);
            label = view.findViewById(R.id.zer_item_label);
        }
    }
}
