package com.btmura.android.reddit;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.TextUtils.TruncateAt;
import android.text.method.LinkMovementMethod;
import android.text.style.TextAppearanceSpan;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class EntityAdapter extends BaseAdapter {

	private final Context context;
	private final ArrayList<Entity> entities;
	private final LayoutInflater inflater;
	private int origLeftPadding;

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

	public View getView(int position, View convertView, ViewGroup parent) {
		TextView tv = (TextView) convertView;
		if (tv == null) {
			tv = (TextView) inflater.inflate(android.R.layout.simple_list_item_activated_1, parent, false);
			origLeftPadding = tv.getPaddingLeft();
		}
		Entity e = getItem(position);
		switch (e.type) {
		case Entity.TYPE_TITLE:
			tv.setSingleLine();
			tv.setEllipsize(TruncateAt.END);
			tv.setText(e.title);
			setPadding(tv, 0);
			break;
			
		case Entity.TYPE_HEADER:
			tv.setMovementMethod(LinkMovementMethod.getInstance());
			tv.setText(getHeader(e));
			setPadding(tv, 0);
			break;
			
		case Entity.TYPE_COMMENT:
			tv.setMovementMethod(LinkMovementMethod.getInstance());
			tv.setText(linkify(e.body));
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
	
	private CharSequence getHeader(Entity e) {
		SpannableStringBuilder builder = null; 
		if (e.selfText == null || e.selfText.isEmpty()) {
			builder = new SpannableStringBuilder();
		} else {
			builder = linkify(e.selfText);
			builder.insert(0, "\n");
			builder.insert(0, "\n");
		}
		builder.insert(0, e.title);
		builder.setSpan(new TextAppearanceSpan(context, android.R.style.TextAppearance_Holo_Large),
				0, e.title.length(), 0);
		return builder;
	}
	
	static Pattern NAMED_LINK_PATTERN = Pattern.compile("(\\[([^\\]]+?)\\]\\(([^\\)]+?)\\))");
	static Pattern LINK_PATTERN = Pattern.compile("http[s]?://([A-Za-z0-9\\./\\-_#\\?&=;,]+)");
	
	private static SpannableStringBuilder linkify(CharSequence text) {
		SpannableStringBuilder builder = new SpannableStringBuilder(text);
		
		Matcher m = NAMED_LINK_PATTERN.matcher(text);
		for (int deleted = 0; m.find(); ) {
			String whole = m.group(1);
			String title = m.group(2);
			String url = m.group(3);
			
			int start = m.start() - deleted;
			int end = m.end() - deleted;
			builder.replace(start, end, title);
			
			if (url.startsWith("/r/")) {
				url = "http://www.reddit.com" + url;
			} else if (!url.startsWith("http://")) {
				url = "http://" + url;
			}
			
			URLSpan span = new URLSpan(url);
			builder.setSpan(span, start, start + title.length(), 0);
			
			deleted += whole.length() - title.length();
		}
		
		m.usePattern(LINK_PATTERN);
		m.reset(builder);
		while (m.find()) {
			URLSpan span = new URLSpan(m.group());
			builder.setSpan(span, m.start(), m.end(), 0);
		}

		return builder;
	}
	
	private void setPadding(TextView tv, int nesting) {
		tv.setPadding((nesting + 1) * origLeftPadding, tv.getPaddingTop(), tv.getPaddingRight(), tv.getPaddingBottom());
	}
}
