package com.btmura.android.reddit;

import java.util.ArrayList;

import android.app.ListFragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannedString;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.EntityListFragment.LoadResult;

public abstract class EntityListFragment<M> extends ListFragment
		implements TaskListener<LoadResult<M>>, OnScrollListener {
	
	public interface LoadResult<M> {
		ArrayList<Entity> getEntities();
		M getMoreKey();
	}

	protected EntityAdapter adapter;
	
	private AsyncTask<Void, Void, LoadResult<M>> task;
	private M moreKey;
	private boolean requestedMore;
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (adapter == null) {
			load(moreKey);
		}
	}
	
	private void load(M moreKey) {
		task = createLoadTask(moreKey);
		task.execute();
	}

	protected abstract AsyncTask<Void, Void, LoadResult<M>> createLoadTask(M moreKey);
	
	public void onPreExecute() {
	}
	
	public void onPostExecute(LoadResult<M> result) {
		moreKey = result.getMoreKey();
		requestedMore = false;
		if (adapter == null) {
			adapter = new EntityAdapter(result.getEntities(), getActivity().getLayoutInflater());
			setListAdapter(adapter);
			setEmptyText(getString(result.getEntities() != null ? R.string.empty : R.string.error));
		} else {
			if (result.getEntities() != null) {
				removeProgressItem();
				adapter.addAll(result.getEntities());
			} else {
				updateProgressItem(R.string.loading_error, false);
			}
		}
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Entity e = adapter.getItem(position);
		switch (e.type) {
		case Entity.TYPE_MORE:
			if (moreKey != null && !requestedMore) {
				updateProgressItem(R.string.loading, true);
				load(moreKey);
				requestedMore = true;
			}
		}
	}

	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		if (totalItemCount <= 0) {
			return;
		}
		int lastVisibleItem = firstVisibleItem + visibleItemCount;
		if (lastVisibleItem >= totalItemCount) {
			if (moreKey != null && !requestedMore) {
				addProgressItem(R.string.loading, true);
				load(moreKey);
				requestedMore = true;
			}
		}
	}
	
	private void addProgressItem(int text, boolean progress) {
		Entity e = new Entity();
		e.type = Entity.TYPE_MORE;
		e.line1 = new SpannedString(getString(text));
		e.progress = progress;
		adapter.add(e);
	}
	
	private void updateProgressItem(int text, boolean progress) {
		Entity e = adapter.getItem(adapter.getCount() - 1);
		e.line1 = new SpannedString(getString(text));
		e.progress = progress;
		adapter.notifyDataSetChanged();
	}
	
	private void removeProgressItem() {
		adapter.remove(adapter.getCount() - 1);
	}
	
	public void onScrollStateChanged(AbsListView view, int scrollState) {
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (task != null) {
			task.cancel(true);
		}
	}
}
