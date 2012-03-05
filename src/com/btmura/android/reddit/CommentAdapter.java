package com.btmura.android.reddit;

import java.util.ArrayList;
import java.util.List;

import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class CommentAdapter extends BaseAdapter {

    private final ArrayList<Comment> items = new ArrayList<Comment>();
    private final LayoutInflater inflater;

    public CommentAdapter(LayoutInflater inflater) {
        this.inflater = inflater;
    }

    public void swapData(List<Comment> newItems) {
        items.clear();
        if (newItems != null) {
            items.ensureCapacity(items.size() + newItems.size());
            items.addAll(newItems);
            notifyDataSetChanged();
        } else {
            notifyDataSetInvalidated();
        }
    }

    public int getCount() {
        return items.size();
    }

    public Comment getItem(int position) {
        return items.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).type;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            v = createView(position, parent);
        }
        setView(position, v);
        return v;
    }

    private View createView(int position, ViewGroup parent) {
        switch (getItemViewType(position)) {
            case Comment.TYPE_HEADER:
                return makeView(R.layout.comment_header_row, parent);

            case Comment.TYPE_COMMENT:
                return makeView(R.layout.comment_row, parent);

            default:
                throw new IllegalArgumentException("Unexpected view type: "
                        + getItemViewType(position));
        }
    }

    private View makeView(int layout, ViewGroup parent) {
        View v = inflater.inflate(layout, parent, false);
        v.setTag(createViewHolder(v));
        return v;
    }

    private static ViewHolder createViewHolder(View v) {
        ViewHolder holder = new ViewHolder();
        holder.title = (TextView) v.findViewById(R.id.title);
        holder.body = (TextView) v.findViewById(R.id.body);
        holder.status = (TextView) v.findViewById(R.id.status);
        return holder;
    }

    static class ViewHolder {
        TextView title;
        TextView body;
        TextView status;
    }

    private void setView(int position, View v) {
        Comment c = getItem(position);
        ViewHolder h = (ViewHolder) v.getTag();
        switch (c.type) {
            case Comment.TYPE_HEADER:
                setHeader(h, c);
                break;

            case Comment.TYPE_COMMENT:
                setComment(h, c);
                break;

            default:
                throw new IllegalArgumentException("Unsupported view type: " + c.type);
        }
    }

    private void setHeader(ViewHolder h, Comment c) {
        h.title.setText(c.title);
        h.body.setMovementMethod(LinkMovementMethod.getInstance());
        h.body.setVisibility(c.body != null && c.body.length() > 0 ? View.VISIBLE : View.GONE);
        h.body.setText(c.body);
        h.status.setText(c.status);
    }

    private void setComment(ViewHolder h, Comment c) {
        h.body.setMovementMethod(LinkMovementMethod.getInstance());
        h.body.setText(c.body);
        h.status.setText(c.status);
        setPadding(h.body, c.nesting);
        setPadding(h.status, c.nesting);
    }

    private static void setPadding(View v, int nesting) {
        v.setPadding(v.getPaddingRight() + nesting * 20, v.getPaddingTop(), v.getPaddingRight(),
                v.getPaddingBottom());
    }
}
