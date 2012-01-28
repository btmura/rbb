package com.btmura.android.reddit;

import java.util.ArrayList;

import android.text.TextUtils.TruncateAt;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class EntityAdapter extends BaseAdapter {
	
	private final ArrayList<Entity> entities;
	private final LayoutInflater inflater;
	private int origLeftPadding;

	public EntityAdapter(ArrayList<Entity> entities, LayoutInflater inflater) {
		this.entities = entities;
		this.inflater = inflater;
	}
	
	public int getCount() {
		return entities != null ? entities.size() : 0;
	}

	public Entity getItem(int position) {
		return entities.get(position);
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
		return Entity.NUM_TYPES;
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
		case Entity.TYPE_TITLE:
		case Entity.TYPE_COMMENT:
		case Entity.TYPE_MORE:
			TextView tv = (TextView) inflater.inflate(android.R.layout.simple_list_item_activated_1, parent, false);
			origLeftPadding = tv.getPaddingLeft();
			return tv;
			
		case Entity.TYPE_HEADER:
			return inflater.inflate(android.R.layout.two_line_list_item, parent, false);
			
		default:
			throw new IllegalArgumentException("Unsupported view type: " + getItemViewType(position));
		}
	}
	
	private void setView(int position, View v) {
		Entity e = getItem(position);
		switch (getItemViewType(position)) {
		case Entity.TYPE_TITLE:
			setTitle(v, e);
			break;
			
		case Entity.TYPE_COMMENT:
			setComment(v, e);
			break;
			
		case Entity.TYPE_MORE:
			setMore(v, e);
			break;
			
		case Entity.TYPE_HEADER:
			setHeader(v, e);
			break;
			
		default:
			throw new IllegalArgumentException("Unsupported view type: " + getItemViewType(position));
		}
	}
	
	private void setTitle(View v, Entity e) {
		TextView tv = (TextView) v;
		tv.setSingleLine();
		tv.setEllipsize(TruncateAt.END);
		tv.setText(e.title);
		setPadding(tv, 0);
	}
	
	private void setComment(View v, Entity e) {
		TextView tv = (TextView) v;
		tv.setMovementMethod(LinkMovementMethod.getInstance());
		tv.setText(e.body);
		setPadding(tv, e.nesting);
	}
	
	private void setMore(View v, Entity e) {
		TextView tv = (TextView) v;
		tv.setText(R.string.load_more);
		setPadding(tv, e.nesting);
	}
	
	private void setHeader(View v, Entity e) {
		TextView tv1 = (TextView) v.findViewById(android.R.id.text1);
		TextView tv2 = (TextView) v.findViewById(android.R.id.text2);
		tv1.setMovementMethod(LinkMovementMethod.getInstance());
		tv2.setMovementMethod(LinkMovementMethod.getInstance());
		tv1.setText(e.title);
		tv2.setText(e.selfText);
	}
	
	private void setPadding(TextView tv, int nesting) {
		tv.setPadding(origLeftPadding + nesting * 20, tv.getPaddingTop(), tv.getPaddingRight(), tv.getPaddingBottom());
	}
}
