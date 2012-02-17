package com.btmura.android.reddit.addsubreddits;

import java.util.ArrayList;
import java.util.List;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.btmura.android.reddit.R;

public class SubredditInfoAdapter extends BaseAdapter {
	
	private final LayoutInflater inflater;
	
	private final ArrayList<SubredditInfo> items = new ArrayList<SubredditInfo>();
	
	public SubredditInfoAdapter(LayoutInflater inflater) {
		this.inflater = inflater;
	}
	
	public void clear() {
		items.clear();
	}
	
	public void addAll(List<SubredditInfo> newItems) {
		items.ensureCapacity(items.size() + newItems.size());
		items.addAll(newItems);
		notifyDataSetChanged();
	}
	
	public int getCount() {
		return items.size();
	}
	
	public SubredditInfo getItem(int position) {
		return items.get(position);
	}
	
	public long getItemId(int position) {
		return position;
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
			v = inflater.inflate(R.layout.sr_info_row, parent, false);
			v.setTag(createViewHolder(v));
		}
		ViewHolder h = (ViewHolder) v.getTag();
		SubredditInfo item = getItem(position);
		h.title.setText(item.title);
		h.status.setText(item.status);
		return v;
	}
	
	static class ViewHolder {
		TextView title;	
		TextView status;
	}
	
	private static ViewHolder createViewHolder(View v) {
		ViewHolder h = new ViewHolder();
		h.title = (TextView) v.findViewById(R.id.title);
		h.status = (TextView) v.findViewById(R.id.info);
		return h;
	}
}
