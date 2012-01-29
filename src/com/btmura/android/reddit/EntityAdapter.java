package com.btmura.android.reddit;

import java.util.ArrayList;

import android.content.Context;
import android.text.TextUtils.TruncateAt;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class EntityAdapter extends BaseAdapter {
	
	private final Context context;
	private final ArrayList<Entity> entities;
	private final LayoutInflater inflater;

	public EntityAdapter(Context context, ArrayList<Entity> entities) {
		this.context = context;
		this.entities = entities;
		this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
			return inflater.inflate(android.R.layout.simple_list_item_activated_1, parent, false);

		case Entity.TYPE_HEADER:
		case Entity.TYPE_COMMENT:
		case Entity.TYPE_MORE:
			return inflater.inflate(R.layout.entity, parent, false);
			
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
			
		case Entity.TYPE_HEADER:
			setHeader(v, e);
			break;
			
		case Entity.TYPE_COMMENT:
			setComment(v, e);
			break;
			
		case Entity.TYPE_MORE:
			setMore(v, e);
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
	}
	
	private void setHeader(View v, Entity e) {
		TextView tv1 = (TextView) v.findViewById(R.id.line1);
		TextView tv2 = (TextView) v.findViewById(R.id.line2);
		TextView tv3 = (TextView) v.findViewById(R.id.line3);
		
		tv1.setMovementMethod(null);
		tv2.setMovementMethod(LinkMovementMethod.getInstance());

		tv1.setTextAppearance(context, android.R.style.TextAppearance_Holo_Large);
		tv2.setTextAppearance(context, android.R.style.TextAppearance_Holo_Medium);
		
		tv1.setText(e.line1);
		tv2.setText(e.line2);
		
		tv1.setVisibility(View.VISIBLE);
		tv2.setVisibility(e.line2 != null && e.line2.length() > 0 ? View.VISIBLE : View.GONE);
		tv3.setVisibility(View.GONE);
	}
	
	private void setComment(View v, Entity e) {
		TextView tv1 = (TextView) v.findViewById(R.id.line1);
		TextView tv2 = (TextView) v.findViewById(R.id.line2);
		TextView tv3 = (TextView) v.findViewById(R.id.line3);
		
		tv1.setMovementMethod(LinkMovementMethod.getInstance());
	
		tv1.setTextAppearance(context, android.R.style.TextAppearance_Holo_Medium);

		tv1.setText(e.line1);

		tv1.setVisibility(View.VISIBLE);
		tv2.setVisibility(View.GONE);
		tv3.setVisibility(View.GONE);
		
		setPadding(tv1, e.nesting);
	}
	
	private void setMore(View v, Entity e) {
		TextView tv1 = (TextView) v.findViewById(R.id.line1);
		TextView tv2 = (TextView) v.findViewById(R.id.line2);
		TextView tv3 = (TextView) v.findViewById(R.id.line3);
		
		tv1.setTextAppearance(context, android.R.style.TextAppearance_Holo_Small);
		tv1.setTextColor(android.R.color.holo_blue_light);
		tv1.setText(R.string.load_more);
		
		tv1.setVisibility(View.VISIBLE);
		tv2.setVisibility(View.GONE);
		tv3.setVisibility(View.GONE);
		
		setPadding(tv1, e.nesting);
	}

	private void setPadding(TextView tv, int nesting) {
		tv.setPadding(tv.getPaddingRight() + nesting * 20, tv.getPaddingTop(), tv.getPaddingRight(), tv.getPaddingBottom());
	}
}
