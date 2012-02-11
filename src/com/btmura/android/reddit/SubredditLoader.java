package com.btmura.android.reddit;

import java.util.ArrayList;
import java.util.List;

import android.content.AsyncTaskLoader;
import android.content.Context;

public class SubredditLoader extends AsyncTaskLoader<List<Subreddit>> {

	private List<Subreddit> subreddits;
	
	public SubredditLoader(Context context) {
		super(context);
	}
	
	@Override
	protected void onStartLoading() {
		if (subreddits != null) {
			deliverResult(subreddits);
		}
		
		if (takeContentChanged() || subreddits == null) {
			forceLoad();
		}
	}
	
	@Override
	public List<Subreddit> loadInBackground() {
		ArrayList<Subreddit> subreddits = new ArrayList<Subreddit>();
		subreddits.add(Subreddit.frontPage(getContext()));
		subreddits.add(Subreddit.multiSubreddit("all"));
		subreddits.add(Subreddit.newInstance("AskReddit"));
		subreddits.add(Subreddit.newInstance("android"));
		subreddits.add(Subreddit.newInstance("askscience"));
		subreddits.add(Subreddit.newInstance("atheism"));
		subreddits.add(Subreddit.newInstance("aww"));
		subreddits.add(Subreddit.newInstance("funny"));
		subreddits.add(Subreddit.newInstance("fitness"));
		subreddits.add(Subreddit.newInstance("gaming"));
		subreddits.add(Subreddit.newInstance("health"));
		subreddits.add(Subreddit.newInstance("humor"));
		subreddits.add(Subreddit.newInstance("IAmA"));
		subreddits.add(Subreddit.newInstance("pics"));
		subreddits.add(Subreddit.newInstance("politics"));
		subreddits.add(Subreddit.newInstance("science"));
		subreddits.add(Subreddit.newInstance("technology"));
		subreddits.add(Subreddit.newInstance("todayilearned"));
		subreddits.add(Subreddit.newInstance("videos"));
		subreddits.add(Subreddit.newInstance("worldnews"));
		subreddits.add(Subreddit.newInstance("WTF"));
		return subreddits;
	}
	
	@Override
	public void deliverResult(List<Subreddit> data) {
		if (isReset()) {
			return;
		}
		subreddits = data;
		super.deliverResult(data);
	}
	
	@Override
	protected void onReset() {
		super.onReset();
		onStopLoading();
		subreddits = null;
	}
	
	@Override
	protected void onStopLoading() {
		super.onStopLoading();
		cancelLoad();
	}
}
