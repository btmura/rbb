package com.btmura.android.reddit.browser;

import java.util.List;

import com.btmura.android.reddit.R;

import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.os.Bundle;

public class CommentListFragment extends ListFragment implements LoaderCallbacks<List<Comment>> {

    private static final String ARG_THING_ID = "i";

    private CommentAdapter adapter;

    public static CommentListFragment newInstance(String thingId) {
        CommentListFragment frag = new CommentListFragment();
        Bundle b = new Bundle(1);
        b.putString(ARG_THING_ID, thingId);
        frag.setArguments(b);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new CommentAdapter(getActivity(), getActivity().getLayoutInflater());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(adapter);
        setListShown(false);
        getLoaderManager().initLoader(0, null, this);
    }

    public Loader<List<Comment>> onCreateLoader(int id, Bundle args) {
        CharSequence url = Urls.commentsUrl(getArguments().getString(ARG_THING_ID));
        return new CommentLoader(getActivity(), url);
    }

    public void onLoadFinished(Loader<List<Comment>> loader, List<Comment> comments) {
        adapter.swapData(comments);
        setEmptyText(getString(comments != null ? R.string.empty : R.string.error));
        setListShown(true);
    }

    public void onLoaderReset(Loader<List<Comment>> loader) {
        adapter.swapData(null);
    }
}
