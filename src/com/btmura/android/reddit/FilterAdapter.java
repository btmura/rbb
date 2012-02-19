package com.btmura.android.reddit;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class FilterAdapter extends BaseAdapter {
	
	public static final int FILTER_HOT = 0;
	public static final int FILTER_NEW = 1;
	public static final int FILTER_CONTROVERSIAL = 2;
	public static final int FILTER_TOP = 3;

	private final LayoutInflater inflater;
	private final ArrayList<String> names = new ArrayList<String>(4);
	private String subreddit;

	public FilterAdapter(Context context) {
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		names.add(context.getString(R.string.filter_hot));
		names.add(context.getString(R.string.filter_new));
		names.add(context.getString(R.string.filter_controversial));
		names.add(context.getString(R.string.filter_top));
	}
	
	public int getCount() {
		return names.size();
	}
	
	public String getItem(int position) {
		return names.get(position);
	}
	
	public long getItemId(int position) {
		return position;
	}
	
	public void setSubreddit(String subreddit) {
		this.subreddit = subreddit;
		notifyDataSetChanged();
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
			v = makeView(parent);
		}
		
		ViewHolder h = (ViewHolder) v.getTag();
		h.line1.setText(subreddit);
		h.line2.setText(getItem(position));
		return v;
	}
	
	private View makeView(ViewGroup parent) {
		View v = inflater.inflate(R.layout.filter_spinner, parent, false);
		ViewHolder h = new ViewHolder();
		h.line1 = (TextView) v.findViewById(R.id.line1);
		h.line2 = (TextView) v.findViewById(R.id.line2);
		v.setTag(h);
		return v;
	}
	
	static class ViewHolder {
		TextView line1;
		TextView line2;
	}
	
	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
			v = inflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
		}
		TextView tv = (TextView) v.findViewById(android.R.id.text1);
		tv.setText(getItem(position));
		return v;
	}
}
