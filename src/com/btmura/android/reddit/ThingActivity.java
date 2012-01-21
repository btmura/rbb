package com.btmura.android.reddit;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;


public class ThingActivity extends Activity implements ThingHolder {

	static final String EXTRA_THING = "thing";
	static final String EXTRA_POSITION = "position";
	
	private static final String THING_TAG = "thing";
	
	private static final String STATE_THING = "thing";
	
	private Thing thing;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.thing);
		
		if (savedInstanceState != null) {
			thing = savedInstanceState.getParcelable(STATE_THING);
		} else {
			thing = getIntent().getParcelableExtra(EXTRA_THING);
		}
		
		FragmentManager manager = getFragmentManager();
		FragmentTransaction transaction = manager.beginTransaction();
		ThingFragment frag = ThingFragment.newInstance();
		transaction.replace(R.id.thing_container, frag, THING_TAG);
		transaction.commit();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(STATE_THING, thing);
	}
	
	public Thing getThing() {
		return thing;
	}
}
