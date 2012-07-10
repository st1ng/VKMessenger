package ru.st1ng.vk.views;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.ImageView;

public class WebImageView extends ImageView {
	
	/**
	 * Caches web images. This solution is only appropriate for a minimal amount of images.
	 * For larger images, the files should be cached locally.
	 */
	final static ConcurrentHashMap<String,SoftReference<Bitmap>> imageCache = new ConcurrentHashMap<String,SoftReference<Bitmap>>();
	
	final static ImageListener defaultListener = new ImageListener() {
		public void onImageLoaded(WebImageView im, Bitmap bm, String url) {
			im.setImageBitmap(bm);
		}
	};
	
	ImageListener listener = null;
	boolean cache = false;
	
	public static ConcurrentHashMap<String,SoftReference<Bitmap>> getImageCache() {
		return imageCache;
	}
	
	public WebImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		_init();
	}

	public WebImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		_init();
	}
	
	public WebImageView(Context context) {
		super(context);
		_init();
	}
	
	public void setCache(boolean cache) {
		this.cache = cache;
	}
	
	public void setImageListener(ImageListener listener) {
		this.listener = listener;
	}
	
	public void setImageFromURL(String imageUrl) {
		
		if (cache && imageCache.contains(imageUrl)) {
			SoftReference<Bitmap> ref = imageCache.get(imageUrl);
			if (ref != null) {
				// use our cached bitmap
				setImageBitmap(ref.get());
				return;
			}
			imageCache.remove(imageUrl);
		}
		
		try {
			// create the URL object
			final URL url = new URL(imageUrl);
			new Thread() {
				public void run() {
					// obtain an input stream of the image
					InputStream is = null;
					try {
						is = (InputStream) url.getContent();
					} 
					catch (IOException e) {}
					
					final InputStream imgStream = is;
					final Bitmap bm = BitmapFactory.decodeStream(imgStream);
					if (WebImageView.this.getHandler() != null) {
						WebImageView.this.post(new Runnable() {
							public void run() {
								// cache the image
								if (WebImageView.this.cache) {
									WebImageView.imageCache.put(url.toExternalForm(), new SoftReference<Bitmap>(bm));
								}
								// call our listener
								if (WebImageView.this.listener != null) {
									WebImageView.this.listener.onImageLoaded(WebImageView.this, bm, url.toExternalForm());
								}
								else {
									WebImageView.defaultListener.onImageLoaded(WebImageView.this, bm, url.toExternalForm());
								}
							}
						});
					}
				}
			}.start();
		} 
		catch (MalformedURLException e) {}
	}
	
	private void _init() {
		
	}
	
	// ---------- Image Load Listener ---------- //
	public static interface ImageListener {
		public void onImageLoaded(WebImageView im, Bitmap bm, String url);
	}
}