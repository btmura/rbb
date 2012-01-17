package com.btmura.android.reddit;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;

public class ThingActivity extends Activity {

	static final String EXTRA_THING = "thing";

	static final String EXTRA_POSITION = "position";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.thing);
		
		Intent intent = getIntent();
		Thing thing = intent.getParcelableExtra(EXTRA_THING);
		int position = intent.getIntExtra(EXTRA_POSITION, -1);
		
		ThingFragment frag = new ThingFragment();
		frag.setThing(thing, position);
		
		FragmentManager manager = getFragmentManager();
		FragmentTransaction transaction = manager.beginTransaction();
		transaction.replace(R.id.thing_container, frag);
		transaction.commit();
	}
}
