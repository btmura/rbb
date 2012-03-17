package com.btmura.android.reddit.search;

import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;

import com.btmura.android.reddit.Provider;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.Provider.Subreddits;
import com.btmura.android.reddit.search.SubredditInfoListFragment.OnSelectedListener;

public class SearchActivity extends Activity implements OnQueryTextListener,
        OnSelectedListener, OnBackStackChangedListener {

    public static final String EXTRA_QUERY = "q";

    private static final String FRAG_SUBREDDITS = "s";
    private static final String FRAG_DETAILS = "d";

    private static final String STATE_QUERY = "q";

    private SearchView sv;
    private View singleContainer;

    private ActionBar bar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_subreddits);

        FragmentManager manager = getFragmentManager();
        manager.addOnBackStackChangedListener(this);

        bar = getActionBar();
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setDisplayShowTitleEnabled(false);
        bar.setDisplayShowCustomEnabled(true);
        bar.setCustomView(R.layout.subreddits_search);

        sv = (SearchView) bar.getCustomView();
        sv.setOnQueryTextListener(this);
        sv.setFocusable(false);

        singleContainer = findViewById(R.id.single_container);

        if (savedInstanceState == null) {
            String q = getIntent().getStringExtra(EXTRA_QUERY);
            if (q != null && !q.trim().isEmpty()) {
                sv.setQuery(q.trim(), false);
                submitQuery(q.trim());
            }
        }
    }

    public boolean onQueryTextSubmit(String query) {
        submitQuery(query);
        return true;
    }

    private void submitQuery(String query) {
        sv.clearFocus();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (singleContainer != null) {
            ft.replace(R.id.single_container, SubredditInfoListFragment.newInstance(query, false),
                    FRAG_SUBREDDITS);
        } else {
            ft.replace(R.id.subreddits_container,
                    SubredditInfoListFragment.newInstance(query, true), FRAG_SUBREDDITS);
            ft.replace(R.id.details_container, DetailsFragment.newInstance(null, -1), FRAG_DETAILS);
        }
        ft.commit();
    }

    public void onSelected(List<SubredditInfo> infos, int position, int event) {
        switch (event) {
            case OnSelectedListener.EVENT_LIST_ITEM_CLICKED:
                handleListItemClicked(infos, position);
                break;

            case OnSelectedListener.EVENT_ACTION_ITEM_CLICKED:
                handleActionItemClicked(infos);
                break;

            default:
                throw new IllegalArgumentException("Unexpected event: " + event);
        }
    }

    private void handleListItemClicked(List<SubredditInfo> infos, int position) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        int containerId = singleContainer != null ? R.id.single_container : R.id.details_container;
        ft.replace(containerId, DetailsFragment.newInstance(infos.get(0), position), FRAG_DETAILS);
        if (singleContainer != null) {
            ft.addToBackStack(null);
        }
        ft.commit();
    }

    private void handleActionItemClicked(List<SubredditInfo> infos) {
        int size = infos.size();
        ContentValues[] values = new ContentValues[size];
        for (int i = 0; i < size; i++) {
            values[i] = new ContentValues(1);
            values[i].put(Subreddits.COLUMN_NAME, infos.get(i).displayName);
        }
        Provider.addMultipleSubredditsInBackground(getApplicationContext(), values);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.subreddit_search, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        DetailsFragment f = getDetailsFragment();
        boolean showDetails = f != null && f.getSubredditInfo() != null;
        menu.findItem(R.id.menu_add).setVisible(showDetails);
        menu.findItem(R.id.menu_view).setVisible(showDetails);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                handleHome();
                return true;

            case R.id.menu_add_front_page:
                handleAddFrontPage();
                return true;

            default:
                return super.onMenuItemSelected(featureId, item);
        }
    }

    private void handleHome() {
        if (singleContainer != null) {
            if (getDetailsFragment() != null) {
                getFragmentManager().popBackStack();
            } else {
                finish();
            }
        } else {
            finish();
        }
    }

    private void handleAddFrontPage() {
        ContentValues values = new ContentValues(1);
        values.put(Subreddits.COLUMN_NAME, "");
        Provider.addSubredditInBackground(getApplicationContext(), values);
    }

    public void onBackStackChanged() {
        DetailsFragment f = getDetailsFragment();
        refreshPosition(f);
        refreshActionBar(f);
    }

    private void refreshPosition(DetailsFragment detailsFrag) {
        if (singleContainer == null) {
            int position = detailsFrag != null ? detailsFrag.getPosition() : -1;
            getSubredditListFragment().setChosenPosition(position);
        }
    }

    private void refreshActionBar(DetailsFragment detailsFrag) {
        if (singleContainer != null) {
            if (detailsFrag != null) {
                bar.setDisplayShowTitleEnabled(true);
                bar.setDisplayShowCustomEnabled(false);
                SubredditInfo info = detailsFrag.getSubredditInfo();
                bar.setTitle(info.title);
            } else {
                bar.setDisplayShowTitleEnabled(false);
                bar.setDisplayShowCustomEnabled(true);
                bar.setTitle(null);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_QUERY, sv.getQuery().toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            sv.setQuery(savedInstanceState.getString(STATE_QUERY), false);
            refreshActionBar(getDetailsFragment());
        }
    }

    private SubredditInfoListFragment getSubredditListFragment() {
        return (SubredditInfoListFragment) getFragmentManager().findFragmentByTag(FRAG_SUBREDDITS);
    }

    private DetailsFragment getDetailsFragment() {
        return (DetailsFragment) getFragmentManager().findFragmentByTag(FRAG_DETAILS);
    }

    public boolean onQueryTextChange(String newText) {
        return false;
    }
}
