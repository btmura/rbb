package com.btmura.android.reddit;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;

public class ThingTabFragment extends Fragment {

	private static final String TAG = "ThingTabFragment";

	private static final String TAB_LINK = "link";	
	private static final String TAB_COMMENTS = "comments";
	
	private ThingHolder thingHolder;
	private TabHost tabHost;
	
	@Override
	public void onAttach(Activity activity) {
		Log.v(TAG, "onAttach");
		super.onAttach(activity);
		thingHolder = (ThingHolder) activity;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.v(TAG, "onCreateView");
		View root = inflater.inflate(R.layout.thing_tab_fragment, container, false);
		setupTabHost(root);
		return root;
	}
	
	private void setupTabHost(View root) {
		tabHost = (TabHost) root.findViewById(android.R.id.tabhost);
		tabHost.setup();

		tabHost.addTab(tabHost.newTabSpec(TAB_LINK)
				.setIndicator(getString(R.string.link))
				.setContent(R.id.link_content));
		
		tabHost.addTab(tabHost.newTabSpec(TAB_COMMENTS)
				.setIndicator(getString(R.string.comments))
				.setContent(R.id.comments_content));
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.v(TAG, "onActivityCreated");
		super.onActivityCreated(savedInstanceState);
		if (savedInstanceState == null) {
			createLinkTab(thingHolder.getThing());
		}
	}
	
	private void createLinkTab(Thing thing) {
		Log.v(TAG, "creating link tab fragment");
		FragmentManager manager = getFragmentManager();
		FragmentTransaction transaction = manager.beginTransaction();
		transaction.replace(R.id.link_content, new ThingWebFragment(), TAB_LINK);
		transaction.commit();
	}
}
