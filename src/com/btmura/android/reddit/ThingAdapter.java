package com.btmura.android.reddit;

import java.util.ArrayList;
import java.util.List;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ThingAdapter extends BaseAdapter {
	
	private final ArrayList<Thing> items = new ArrayList<Thing>();
	private final LayoutInflater inflater;

	public ThingAdapter(LayoutInflater inflater) {
		this.inflater = inflater;
	}

	public void swapData(List<Thing> newItems) {
		items.clear();
		if (newItems != null) {
			items.ensureCapacity(items.size() + newItems.size());
			items.addAll(newItems);
			notifyDataSetChanged();
		} else {
			notifyDataSetInvalidated();
		}
	}
	
	public int getCount() {
		return items.size();
	}

	public Thing getItem(int position) {
		return items.get(position);
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
		return 2;
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
		case Thing.TYPE_THING:
			return makeView(R.layout.thing_row, parent);
	
		case Thing.TYPE_MORE:
			return makeView(R.layout.thing_more_row, parent);

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
		holder.title = (TextView) v.findViewById(R.id.title);
		holder.status = (TextView) v.findViewById(R.id.status);
		holder.progress = (ProgressBar) v.findViewById(R.id.progress);
		return holder;
	}
	
	static class ViewHolder {
		TextView title;
		TextView status;
		ProgressBar progress;
	}

	private void setView(int position, View v) {
		Thing e = getItem(position);
		ViewHolder h = (ViewHolder) v.getTag();
		switch (e.type) {
		case Thing.TYPE_THING:
			setTitle(h, e);
			break;
						
		case Thing.TYPE_MORE:
			setMore(h, e);
			break;
			
		default:
			throw new IllegalArgumentException("Unsupported view type: " + getItemViewType(position));
		}
	}
	
	private void setTitle(ViewHolder h, Thing t) {
		h.title.setText(t.title);
		h.status.setText(t.status);
	}
	
	private void setMore(ViewHolder h, Thing e) {
	}
}
