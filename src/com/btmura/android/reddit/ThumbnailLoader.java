package com.btmura.android.reddit;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;
import android.view.View;

import com.btmura.android.reddit.ThingAdapter.ViewHolder;

public class ThumbnailLoader {
	
	private static final ThumbnailLoader INSTANCE = new ThumbnailLoader();
	
	private final BitmapCache bitmapCache = new BitmapCache(30);
	
	private final LoadTaskCache taskCache = new LoadTaskCache(10);
	
	public static final ThumbnailLoader getInstance() {
		return INSTANCE;
	}
	
	private ThumbnailLoader() {
	}

	public void cancelTasks() {
		taskCache.evictAll();
	}
	
	public void setThumbnail(String url, ThingAdapter.ViewHolder holder, Resources res) {
		if (url == null || url.isEmpty() || "default".equals(url) || "self".equals(url) || "nsfw".equals(url)) {
			holder.thumbnail.setVisibility(View.GONE);
			holder.thumbnail.setContentDescription(null);
			holder.thumbnail.setTag(null);
		} else {
			holder.thumbnail.setVisibility(View.VISIBLE);
			holder.thumbnail.setContentDescription(url);
			holder.thumbnail.setTag(url);
			Bitmap b = bitmapCache.get(url);
			if (b != null) {
				holder.thumbnail.setImageBitmap(b);
			} else {
				holder.thumbnail.setImageResource(R.drawable.ic_launcher);
				if (taskCache.get(url) == null) {
					LoadTask task = new LoadTask(url, holder, res);
					taskCache.put(url, task);
					task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
				}
			}
		}
	}
	
	static void replaceThumbnail(ThingAdapter.ViewHolder holder, Resources res, Bitmap b) {
		if (b != null) {
			holder.thumbnail.setImageBitmap(b);
		}
	}

	static class BitmapCache extends LruCache<String, Bitmap> {
		public BitmapCache(int size) {
			super(size);
		}
		
		@Override
		protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
			super.entryRemoved(evicted, key, oldValue, newValue);
			oldValue.recycle();
		}
	}
	
	static class LoadTaskCache extends LruCache<String, LoadTask> {
		public LoadTaskCache(int size) {
			super(size);
		}
		
		@Override
		protected void entryRemoved(boolean evicted, String key, LoadTask oldValue, LoadTask newValue) {
			super.entryRemoved(evicted, key, oldValue, newValue);
			oldValue.cancel(true);
		}
	}
	
	class LoadTask extends AsyncTask<Void, Void, Bitmap> {
		
		private static final String TAG = "LoadTask";
		
		private final String url;
		private final WeakReference<ThingAdapter.ViewHolder> holder;
		private final WeakReference<Resources> res;
		
		LoadTask(String url, ThingAdapter.ViewHolder holder, Resources res) {
			this.url = url;
			this.holder = new WeakReference<ThingAdapter.ViewHolder>(holder);
			this.res = new WeakReference<Resources>(res);
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
			taskCache.remove(url);
			if (b != null) {
				bitmapCache.put(url, b);
				ViewHolder h = holder.get();
				Resources r = res.get();
				if (h != null && r != null && url.equals(h.thumbnail.getTag())) {
					h.thumbnail.setImageBitmap(b);
				}
			}
		}
	}
}
