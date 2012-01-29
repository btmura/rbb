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
		case Entity.TYPE_MORE:
			return inflater.inflate(android.R.layout.simple_list_item_activated_1, parent, false);
			
		case Entity.TYPE_HEADER:
			View v = inflater.inflate(R.layout.entity_three, parent, false);
			v.setTag(createViewHolder(v));
			return v;
			
		case Entity.TYPE_COMMENT:
			v = inflater.inflate(R.layout.entity_two, parent, false);
			v.setTag(createViewHolder(v));
			return v;
			
		default:
			throw new IllegalArgumentException();
		}
	}
	
	private static ViewHolder createViewHolder(View v) {
		ViewHolder holder = new ViewHolder();
		holder.line1 = (TextView) v.findViewById(R.id.line1);
		holder.line2 = (TextView) v.findViewById(R.id.line2);
		holder.line3 = (TextView) v.findViewById(R.id.line3);
		return holder;
	}
	
	static class ViewHolder {
		TextView line1;
		TextView line2;
		TextView line3;
	}
	
	private void setView(int position, View v) {
		Entity e = getItem(position);
		ViewHolder h = (ViewHolder) v.getTag();
		switch (e.type) {
		case Entity.TYPE_TITLE:
			setTitle(v, e);
			break;
			
		case Entity.TYPE_HEADER:
			setHeader(h, e);
			break;
			
		case Entity.TYPE_COMMENT:
			setComment(h, e);
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
	
	private void setHeader(ViewHolder h, Entity e) {
		h.line1.setTextAppearance(context, android.R.style.TextAppearance_Holo_Large);
		h.line2.setTextAppearance(context, android.R.style.TextAppearance_Holo_Medium);
		h.line3.setTextAppearance(context, android.R.style.TextAppearance_Holo_Small);
		
		h.line1.setText(e.line1);
		h.line2.setText(e.line2);
		h.line3.setText(e.line3);
		
		h.line2.setMovementMethod(LinkMovementMethod.getInstance());
		h.line2.setVisibility(e.line3 != null && e.line3.length() > 0 ? View.VISIBLE : View.GONE);
	}
	
	private void setComment(ViewHolder h, Entity e) {
		h.line1.setMovementMethod(LinkMovementMethod.getInstance());
		
		h.line1.setTextAppearance(context, android.R.style.TextAppearance_Holo_Medium);
		h.line2.setTextAppearance(context, android.R.style.TextAppearance_Holo_Small);
		
		h.line1.setText(e.line1);
		h.line2.setText(e.line2);

		h.line1.setVisibility(View.VISIBLE);
		h.line2.setVisibility(View.VISIBLE);
		
		setPadding(h.line1, e.nesting);
		setPadding(h.line2, e.nesting);
	}
	
	private void setMore(View v, Entity e) {
		TextView tv = (TextView) v;
		tv.setText(R.string.load_more);
		tv.setTextAppearance(context, R.style.LoadMoreText);
		setPadding(tv, e.nesting);
	}

	private static void setPadding(TextView tv, int nesting) {
		tv.setPadding(tv.getPaddingRight() + nesting * 20, tv.getPaddingTop(), tv.getPaddingRight(), tv.getPaddingBottom());
	}
}
