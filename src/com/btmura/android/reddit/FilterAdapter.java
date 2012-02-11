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
			v = inflater.inflate(R.layout.spinner_item, parent, false);
		}
		
		TextView tv1 = (TextView) v.findViewById(R.id.line1);
		tv1.setText(subreddit);
		
		TextView tv2 = (TextView) v.findViewById(R.id.line2);
		tv2.setText(getItem(position));
		
		return v;
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
