package com.btmura.android.reddit;


import android.os.AsyncTask;
import android.os.Bundle;

public class CommentListFragment extends EntityListFragment<Void> {

	private static final String ARG_THING = "thing";

	public static CommentListFragment newInstance(Entity thing) {
		CommentListFragment frag = new CommentListFragment();
		Bundle b = new Bundle(1);
		b.putParcelable(ARG_THING, thing);
		frag.setArguments(b);
		return frag;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}
	
	@Override
	protected AsyncTask<Void, Void, LoadResult<Void>> createLoadTask(Void moreKey) {
		Entity thing = getArguments().getParcelable(ARG_THING);
		CharSequence url = Urls.commentsUrl(thing.getId());
		return new CommentLoaderTask(getActivity().getApplicationContext(), url, this);
	}
}
