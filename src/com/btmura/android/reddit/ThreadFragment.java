package com.btmura.android.reddit;

import android.app.ListFragment;
import android.os.Bundle;
import android.widget.ArrayAdapter;

public class ThreadFragment extends ListFragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1);
		adapter.add("Thread 1");
		setListAdapter(adapter);
	}
}

