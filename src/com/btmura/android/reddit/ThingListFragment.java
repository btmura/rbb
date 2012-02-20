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
	
	interface OnThingSelectedListener {
		void onThingSelected(Thing thing, int position);
	}
	
	private ThingAdapter adapter;

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
		adapter = new ThingAdapter(getActivity(), getActivity().getLayoutInflater());
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		ListView list = (ListView) view.findViewById(android.R.id.list);
		boolean singleChoice = getArguments().getBoolean(ARG_SINGLE_CHOICE);
		list.setChoiceMode(singleChoice ? ListView.CHOICE_MODE_SINGLE : ListView.CHOICE_MODE_NONE);
		list.setOnScrollListener(this);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setListAdapter(adapter);
		setListShown(false);
		getLoaderManager().initLoader(0, null, this);
	}
	
	public Loader<List<Thing>> onCreateLoader(int id, Bundle args) {
		Subreddit sr = getArguments().getParcelable(ARG_SUBREDDIT);
		int filter = getArguments().getInt(ARG_FILTER);
		CharSequence url = Urls.subredditUrl(sr, filter, null);
		return new ThingLoader(getActivity(), url);
	}
	
	public void onLoadFinished(Loader<List<Thing>> loader, List<Thing> things) {
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
		Thing e = adapter.getItem(position);
		switch (e.type) {
		case Thing.TYPE_THING:
			getListener().onThingSelected(adapter.getItem(position), position);
			break;
		}
	}

	public void onScrollStateChanged(AbsListView view, int scrollState) {
	}
	
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
	}
	
	private OnThingSelectedListener getListener() {
		return (OnThingSelectedListener) getActivity();
	}
}

