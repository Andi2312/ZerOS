package com.zeroc.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** List adapter with large letter headers. */
public class AppListAdapter extends BaseAdapter {

    public interface Listener {
        void onAppClick(AppEntry entry);

        void onAppLongClick(AppEntry entry);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_APP = 1;

    private final LayoutInflater mInflater;
    private final Listener mListener;
    private final List<Object> mRows = new ArrayList<>();

    public AppListAdapter(Context context, Listener listener) {
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
    public int getCount() {
        return mRows.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return mRows.get(position) instanceof AppEntry ? TYPE_APP : TYPE_HEADER;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Object row = mRows.get(position);
        if (row instanceof String) {
            TextView header;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.app_header, parent, false);
            }
            header = (TextView) convertView;
            header.setText((String) row);
            return convertView;
        }

        final AppEntry entry = (AppEntry) row;
        View view = convertView;
        if (view == null) {
            view = mInflater.inflate(R.layout.app_row, parent, false);
        }
        ImageView icon = view.findViewById(R.id.app_icon);
        TextView label = view.findViewById(R.id.app_label);
        icon.setImageDrawable(entry.icon);
        label.setText(entry.label);
        view.setContentDescription(entry.label);
        view.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onAppClick(entry);
            }
        });
        view.setOnLongClickListener(v -> {
            if (mListener != null) {
                mListener.onAppLongClick(entry);
            }
            return true;
        });
        return view;
    }
}
