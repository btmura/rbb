package com.btmura.android.reddit;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.btmura.android.reddit.ThingFragment.OnThingPartSelectedListener;

public class ThingActivity extends Activity implements OnThingPartSelectedListener {

	static final String EXTRA_THING = "thing";

	static final String EXTRA_POSITION = "position";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.thing);
		
		Intent intent = getIntent();
		Thing thing = intent.getParcelableExtra(EXTRA_THING);
		int position = intent.getIntExtra(EXTRA_POSITION, -1);
			
		ThingCommentsFragment frag = new ThingCommentsFragment();
		frag.setThing(thing, position);

		FragmentManager manager = getFragmentManager();
		FragmentTransaction transaction = manager.beginTransaction();
		transaction.replace(R.id.thing_container, frag);
		transaction.commit();
	}
	
	public void onThingPartSelected(Thing thing, int position, ThingPart part) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(part.value));
		startActivity(intent);
	}
}
