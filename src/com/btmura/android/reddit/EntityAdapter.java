package com.btmura.android.reddit;

import java.util.ArrayList;

import android.text.TextUtils.TruncateAt;
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

	public View getView(int position, View convertView, ViewGroup parent) {
		TextView tv = (TextView) convertView;
		if (tv == null) {
			tv = (TextView) inflater.inflate(android.R.layout.simple_list_item_activated_1, parent, false);
			origLeftPadding = tv.getPaddingLeft();
		}
		Entity e = getItem(position);
		switch (e.type) {
		case Entity.TYPE_TITLE:
			tv.setText(e.title);
			tv.setSingleLine();
			tv.setEllipsize(TruncateAt.END);
			setPadding(tv, 0);
			break;
			
		case Entity.TYPE_HEADER:
			tv.setText(getThingText(e));
			setPadding(tv, 0);
			break;
			
		case Entity.TYPE_COMMENT:
			tv.setText(e.body);
			setPadding(tv, e.nesting);
			break;
			
		case Entity.TYPE_MORE:
			tv.setText(R.string.load_more);
			setPadding(tv, e.nesting);
			break;
			
		default:
			throw new IllegalArgumentException("Unsupported type:" + e.type);
		}
		return tv;
	}
	
	private CharSequence getThingText(Entity e) {
		if (e.selfText == null || e.selfText.isEmpty()) {
			return e.title;
		}
		return new StringBuilder(e.title).append("\n\n").append(e.selfText);
	}
	
	private void setPadding(TextView tv, int nesting) {
		tv.setPadding((nesting + 1) * origLeftPadding, tv.getPaddingTop(), tv.getPaddingRight(), tv.getPaddingBottom());
	}
}
