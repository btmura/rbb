package com.btmura.android.reddit;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;
import android.view.View;
import android.widget.ImageView;

public class ThumbnailLoader {
	
	private static final String TAG = "ThumbnailLoader";

	private final BitmapCache bitmapCache;
	
	public ThumbnailLoader(int size) {
		bitmapCache = new BitmapCache(30);
	}
	
	public void setThumbnail(ImageView v, String url) {
		Bitmap b = bitmapCache.get(url);
		if (b != null) {
			v.setImageBitmap(b);
		} else {						
			int dps = (int) (70 * v.getResources().getDisplayMetrics().density);
			v.setMinimumWidth(dps);
			v.setMinimumHeight(dps);
			v.setImageResource(R.drawable.ic_launcher);
			
			LoadThumbnailTask task = (LoadThumbnailTask) v.getTag();
			if (task == null || !url.equals(task.url)) {
				if (task != null) {
					task.cancel(true);
				}
				task = new LoadThumbnailTask(v, url);
				v.setTag(task);
				task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
		}
		v.setVisibility(View.VISIBLE);
	}
	
	public void clearThumbnail(ImageView v) {
		LoadThumbnailTask task = (LoadThumbnailTask) v.getTag();
		if (task != null) {
			task.cancel(true);
			v.setTag(null);
		}
		v.setVisibility(View.GONE);
	}
	
	public void clearCache() {
		bitmapCache.evictAll();
	}
	
	static class BitmapCache extends LruCache<String, Bitmap> {
		BitmapCache(int size) {
			super(size);
		}
	}
	
	class LoadThumbnailTask extends AsyncTask<Void, Void, Bitmap> {
		
		private final WeakReference<ImageView> ref;
		private final String url;
		
		LoadThumbnailTask(ImageView v, String url) {
			this.ref = new WeakReference<ImageView>(v);
			this.url = url;
		}
		
		@Override
		protected Bitmap doInBackground(Void... params) {
			HttpURLConnection conn = null;
			try {
				URL u = new URL(url);
				conn = (HttpURLConnection) u.openConnection();
				return BitmapFactory.decodeStream(conn.getInputStream());
			} catch (MalformedURLException e) {
				Log.e(TAG, url, e);
			} catch (IOException e) {
				Log.e(TAG, url, e);
			} finally {
				if (conn != null) {
					conn.disconnect();
				}
			}
			return null;
		}	
		
		@Override
		protected void onPostExecute(Bitmap b) {
			if (b != null) {
				bitmapCache.put(url, b);
			}
			ImageView v = ref.get();
			if (v != null && equals(v.getTag())) {
				if (b != null) {
					v.setImageBitmap(b);
				}
				v.setTag(null);
			}
		}
	}
}
