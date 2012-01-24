package com.btmura.android.reddit;

public interface TaskListener {
	void onPreExecute();
	void onProgressUpdate();
	void onPostExecute();
}
