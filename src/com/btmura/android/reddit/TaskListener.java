package com.btmura.android.reddit;

public interface TaskListener<T> {
	void onPreExecute();
	void onPostExecute(T result);
}
