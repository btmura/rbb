package com.btmura.android.reddit;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.EditText;

public class AddSubredditFragment extends DialogFragment {
	
	interface OnSubredditAddedListener {
		void onSubredditAdded(String name);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Holo_Light_Dialog);
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final EditText e = new EditText(getActivity());
		e.setSingleLine();
		return new AlertDialog.Builder(getActivity())
				.setTitle(R.string.dialog_add_subreddit)
				.setView(e)
				.setNegativeButton(android.R.string.cancel, null)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						((OnSubredditAddedListener) getActivity()).onSubredditAdded(e.getText().toString().trim());
					}
				})
				.create();
	}
}
