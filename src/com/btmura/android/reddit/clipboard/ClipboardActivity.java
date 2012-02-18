package com.btmura.android.reddit.clipboard;

import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class ClipboardActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	
		Intent intent = getIntent();
		if (intent != null) {
			String text = intent.getStringExtra(Intent.EXTRA_TEXT);
			if (text != null) {
				ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
				manager.setText(text);
				Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
			}
		}
		
		finish();
	}
}
