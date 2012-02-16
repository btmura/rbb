package com.btmura.android.reddit;

import java.util.ArrayList;
import java.util.List;

import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.btmura.android.reddit.R;

public class EntityAdapter extends BaseAdapter {
	
	private final ArrayList<Entity> entities;
	private final LayoutInflater inflater;

	public EntityAdapter(ArrayList<Entity> entities, LayoutInflater inflater) {
		this.entities = entities;
		this.inflater = inflater;
	}

	public void add(Entity entity) {
		this.entities.add(entity);
		notifyDataSetChanged();
	}
	
	public void addAll(List<Entity> entities) {
		this.entities.addAll(entities);
		notifyDataSetChanged();
	}
	
	public void remove(int index) {
		this.entities.remove(index);
		notifyDataSetChanged();
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
		case Entity.TYPE_THING:
		case Entity.TYPE_COMMENT:
			return makeView(R.layout.entity_two, parent);
			
		case Entity.TYPE_HEADER:
			return makeView(R.layout.entity_three, parent);
			
		case Entity.TYPE_MORE:
			return makeView(R.layout.entity_progress, parent);

		default:
			throw new IllegalArgumentException();
		}
	}
	
	private View makeView(int layout, ViewGroup parent) {
		View v = inflater.inflate(layout, parent, false);
		v.setTag(createViewHolder(v));
		return v;
	}
	
	private static ViewHolder createViewHolder(View v) {
		ViewHolder holder = new ViewHolder();
		holder.line1 = (TextView) v.findViewById(R.id.line1);
		holder.line2 = (TextView) v.findViewById(R.id.line2);
		holder.line3 = (TextView) v.findViewById(R.id.line3);
		holder.progress = (ProgressBar) v.findViewById(R.id.progress);
		return holder;
	}
	
	static class ViewHolder {
		TextView line1;
		TextView line2;
		TextView line3;
		ProgressBar progress;
	}

	private void setView(int position, View v) {
		Entity e = getItem(position);
		ViewHolder h = (ViewHolder) v.getTag();
		switch (e.type) {
		case Entity.TYPE_THING:
			setTitle(h, e);
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
	
	private void setTitle(ViewHolder h, Entity e) {
		h.line1.setText(e.line1);
		h.line2.setText(e.line2);
	}
	
	private void setHeader(ViewHolder h, Entity e) {
		h.line1.setText(e.line1);
		h.line2.setText(e.line2);
		h.line3.setText(e.line3);
		
		h.line2.setMovementMethod(LinkMovementMethod.getInstance());
		h.line2.setVisibility(e.line3 != null && e.line3.length() > 0 ? View.VISIBLE : View.GONE);
	}
	
	private void setComment(ViewHolder h, Entity e) {
		h.line1.setMovementMethod(LinkMovementMethod.getInstance());
		h.line1.setText(e.line1);
		h.line2.setText(e.line2);

		setPadding(h.line1, e.nesting);
		setPadding(h.line2, e.nesting);
	}
	
	private void setMore(ViewHolder h, Entity e) {
		h.line1.setText(e.line1);
		h.progress.setVisibility(e.progress ? View.VISIBLE : View.GONE);
		setPadding(h.line1, e.nesting);
	}

	private static void setPadding(View v, int nesting) {
		v.setPadding(v.getPaddingRight() + nesting * 20, v.getPaddingTop(), v.getPaddingRight(), v.getPaddingBottom());
	}
}
