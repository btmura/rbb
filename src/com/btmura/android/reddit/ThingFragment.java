package com.btmura.android.reddit;

public interface ThingFragment {
	
	interface OnThingPartSelectedListener {
		void onThingPartSelected(Thing thing, int position, ThingPart part);
	}
	
	void setThing(Thing thing, int position);
	
	Thing getThing();
	
	int getThingPosition();
	
	boolean isAdded();
}

