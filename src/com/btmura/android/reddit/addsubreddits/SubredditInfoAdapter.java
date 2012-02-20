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
	
	private final ArrayList<SubredditInfo> items = new ArrayList<SubredditInfo>();
	private final LayoutInflater inflater;
	private final boolean singleChoice;
	private int chosenPosition = -1;
	
	public SubredditInfoAdapter(LayoutInflater inflater, boolean singleChoice) {
		this.inflater = inflater;
		this.singleChoice = singleChoice;
	}
	
	public void swapData(List<SubredditInfo> newItems) {
		items.clear();
		if (newItems != null) {
			items.ensureCapacity(items.size() + newItems.size());
			items.addAll(newItems);
			notifyDataSetChanged();
		} else {
			notifyDataSetInvalidated();
		}
	}
	
	public void setChosenPosition(int position) {
		chosenPosition = position;
	}
	
	public int getChosenPosition() {
		return chosenPosition;
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
		v.setBackgroundResource(singleChoice && position == chosenPosition 
				? R.drawable.selector_chosen 
				: R.drawable.selector_normal);
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
