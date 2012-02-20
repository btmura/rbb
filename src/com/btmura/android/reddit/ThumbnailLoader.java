package com.btmura.android.reddit;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import com.btmura.android.reddit.ThingAdapter.ViewHolder;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;

public class ThumbnailLoader {
	
	private static final ThumbnailLoader INSTANCE = new ThumbnailLoader();
	
	private final ThumbnailCache cache = new ThumbnailCache(30);
	
	public static final ThumbnailLoader getInstance() {
		return INSTANCE;
	}
	
	private ThumbnailLoader() {
	}

	public void load(String url, ThingAdapter.ViewHolder holder, Resources res) {
		if (url == null || url.isEmpty() || "default".equals(url) || "self".equals(url) || "nsfw".equals(url)) {
			holder.title.setCompoundDrawables(null, null, null, null);
		} else {
			holder.title.setTag(url);
			Bitmap b = cache.get(url);
			if (b != null) {
				setThumbnail(holder, res, b);
			} else {
				Drawable d = res.getDrawable(R.drawable.ic_launcher);
				d.setBounds(0, 0, 70, 70);
				holder.title.setCompoundDrawables(d, null, null, null);
				new LoadThumbnailTask(url, holder, res).execute();
			}
		}
	}
	
	static void setThumbnail(ThingAdapter.ViewHolder holder, Resources res, Bitmap b) {
		Drawable d = null;
		if (b != null) {
			d = new BitmapDrawable(res, b);
			d.setBounds(0, 0, b.getWidth(), b.getHeight());
			
		}
		holder.title.setCompoundDrawables(d, null, null, null);
	}

	static class ThumbnailCache extends LruCache<String, Bitmap> {
		public ThumbnailCache(int size) {
			super(size);
		}
		
		@Override
		protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
			super.entryRemoved(evicted, key, oldValue, newValue);
			oldValue.recycle();
		}
	}
	
	class LoadThumbnailTask extends AsyncTask<Void, Void, Bitmap> {
		
		private static final String TAG = "LoadThumbnailTask";
		
		private final String url;
		private final WeakReference<ThingAdapter.ViewHolder> holder;
		private final WeakReference<Resources> res;
		
		LoadThumbnailTask(String url, ThingAdapter.ViewHolder holder, Resources res) {
			this.url = url;
			this.holder = new WeakReference<ThingAdapter.ViewHolder>(holder);
			this.res = new WeakReference<Resources>(res);
		}

		@Override
		protected Bitmap doInBackground(Void... params) {
			HttpURLConnection conn = null;
			try {
				URL u = new URL(url);
				Log.v(TAG, url);			
				
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
			cache.put(url, b);
			ViewHolder h = holder.get();
			Resources r = res.get();
			if (h != null && r != null && url.equals(h.title.getTag())) {
				setThumbnail(h, r, b);
			}
		}
	}
}
