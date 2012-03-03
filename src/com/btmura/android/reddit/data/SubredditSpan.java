package com.btmura.android.reddit.data;

import android.content.Context;
import android.content.Intent;
import android.text.style.ClickableSpan;
import android.view.View;

import com.btmura.android.reddit.MainActivity;

public class SubredditSpan extends ClickableSpan {

	private final String sr;
	
	public SubredditSpan(String sr) {
		this.sr = sr;
	}
	
	@Override
	public void onClick(View widget) {
		Context c = widget.getContext();
		Intent i = new Intent(c, MainActivity.class);
		i.putExtra(MainActivity.EXTRA_SUBREDDIT, sr);
		c.startActivity(i);
	}
}
