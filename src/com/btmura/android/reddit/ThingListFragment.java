package com.btmura.android.reddit;

import java.util.List;

import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

public class ThingListFragment extends ListFragment implements LoaderCallbacks<List<Thing>>, OnScrollListener {
	
	private static final String ARG_SUBREDDIT = "s";
	private static final String ARG_FILTER = "f";
	private static final String ARG_SINGLE_CHOICE = "c";
	
	private static final String STATE_CHOSEN = "s";
	
	private static final String LOADER_ARG_MORE_KEY = "m";
	
	interface OnThingSelectedListener {
		void onThingSelected(Thing thing, int position);
	}
	
	private ThingAdapter adapter;
	private boolean scrollLoading;

	public static ThingListFragment newInstance(Subreddit sr, int filter, boolean singleChoice) {
		ThingListFragment frag = new ThingListFragment();
		Bundle b = new Bundle(3);
		b.putParcelable(ARG_SUBREDDIT, sr);
		b.putInt(ARG_FILTER, filter);
		b.putBoolean(ARG_SINGLE_CHOICE, singleChoice);
		frag.setArguments(b);
		return frag;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		adapter = new ThingAdapter(getActivity(), getActivity().getLayoutInflater(),
				getArguments().getBoolean(ARG_SINGLE_CHOICE));
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		ListView list = (ListView) view.findViewById(android.R.id.list);
		list.setOnScrollListener(this);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		adapter.setChosenPosition(savedInstanceState != null ? savedInstanceState.getInt(STATE_CHOSEN) : -1);
		setListAdapter(adapter);
		setListShown(false);
		getLoaderManager().initLoader(0, null, this);
	}
	
	public Loader<List<Thing>> onCreateLoader(int id, Bundle args) {
		Subreddit sr = getArguments().getParcelable(ARG_SUBREDDIT);
		int filter = getArguments().getInt(ARG_FILTER);
		String moreKey = args != null ? args.getString(LOADER_ARG_MORE_KEY) : null;
		CharSequence url = Urls.subredditUrl(sr, filter, moreKey);
		return new ThingLoader(getActivity(), url, args != null ? adapter.getItems() : null);
	}
	
	public void onLoadFinished(Loader<List<Thing>> loader, List<Thing> things) {
		scrollLoading = false;
		adapter.swapData(things);
		setEmptyText(getString(things != null ? R.string.empty : R.string.error));
		setListShown(true);
	}
	
	public void onLoaderReset(Loader<List<Thing>> loader) {
		adapter.swapData(null);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		adapter.setChosenPosition(position);
		adapter.notifyDataSetChanged();
		Thing t = adapter.getItem(position);
		switch (t.type) {
		case Thing.TYPE_THING:
			getListener().onThingSelected(adapter.getItem(position), position);
			break;
		}
	}

	public void onScrollStateChanged(AbsListView view, int scrollState) {
		
	}
	
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		if (scrollLoading) {
			return;
		}
		if (firstVisibleItem + visibleItemCount >= totalItemCount) {
			Loader<List<Thing>> loader = getLoaderManager().getLoader(0);
			if (loader != null) {
				if (!adapter.isEmpty()) {
					Thing t = adapter.getItem(adapter.getCount() - 1);
					if (t.type == Thing.TYPE_MORE) {
						scrollLoading = true;
						Bundle b = new Bundle(1);
						b.putString(LOADER_ARG_MORE_KEY, t.moreKey);
						getLoaderManager().restartLoader(0, b, this);
					}
				}
			}
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(STATE_CHOSEN, adapter.getChosenPosition());
	}
	
	public void setChosenPosition(int position) {
		if (position != adapter.getChosenPosition()) {
			adapter.setChosenPosition(position);
			adapter.notifyDataSetChanged();
		}
	}
	
	private OnThingSelectedListener getListener() {
		return (OnThingSelectedListener) getActivity();
	}
}

