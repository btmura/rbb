package com.btmura.android.reddit;

public interface TaskListener<U, RS> {
	void onPreExecute();
	void onProgressUpdate(U[] updates);
	void onPostExecute(RS result);
}
