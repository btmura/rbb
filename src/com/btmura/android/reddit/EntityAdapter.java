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
			View v = inflater.inflate(R.layout.entity, parent, false);
			v.setTag(createViewHolder(v));
			return v;
			
		default:
			throw new IllegalArgumentException("Unsupported view type: " + getItemViewType(position));
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
		switch (getItemViewType(position)) {
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
			setMore(h, e);
			break;
			

		default:
			throw new IllegalArgumentException("Unsupported view type: " + getItemViewType(position));
		}
	}
	
	private static void setTitle(View v, Entity e) {
		TextView tv = (TextView) v;
		tv.setSingleLine();
		tv.setEllipsize(TruncateAt.END);
		tv.setText(e.title);
	}
	
	private void setHeader(ViewHolder h, Entity e) {
		h.line1.setMovementMethod(null);
		h.line2.setMovementMethod(LinkMovementMethod.getInstance());

		h.line1.setTextAppearance(context, android.R.style.TextAppearance_Holo_Large);
		h.line2.setTextAppearance(context, android.R.style.TextAppearance_Holo_Medium);
		
		h.line1.setText(e.line1);
		h.line2.setText(e.line2);
		
		h.line1.setVisibility(View.VISIBLE);
		h.line2.setVisibility(e.line2 != null && e.line2.length() > 0 ? View.VISIBLE : View.GONE);
		h.line3.setVisibility(View.GONE);
	}
	
	private void setComment(ViewHolder h, Entity e) {
		h.line1.setMovementMethod(LinkMovementMethod.getInstance());
		
		h.line1.setTextAppearance(context, android.R.style.TextAppearance_Holo_Medium);

		h.line1.setText(e.line1);

		h.line1.setVisibility(View.VISIBLE);
		h.line2.setVisibility(View.GONE);
		h.line3.setVisibility(View.GONE);
		
		setPadding(h.line1, e.nesting);
	}
	
	private void setMore(ViewHolder h, Entity e) {
		h.line1.setTextAppearance(context, android.R.style.TextAppearance_Holo_Small);
		h.line1.setText(R.string.load_more);
		
		h.line1.setVisibility(View.VISIBLE);
		h.line2.setVisibility(View.GONE);
		h.line3.setVisibility(View.GONE);
		
		setPadding(h.line1, e.nesting);
	}

	private static void setPadding(TextView tv, int nesting) {
		tv.setPadding(tv.getPaddingRight() + nesting * 20, tv.getPaddingTop(), tv.getPaddingRight(), tv.getPaddingBottom());
	}
}
