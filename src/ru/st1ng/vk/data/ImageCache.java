package ru.st1ng.vk.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Iterator;

import ru.st1ng.vk.R;
import ru.st1ng.vk.VKApplication;
import ru.st1ng.vk.model.User;
import ru.st1ng.vk.util.ImageUtil;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Environment;


public class ImageCache {

	
	private HashMap<String, SoftReference<Bitmap>> photosMap;
	private Bitmap selfSmallImage;
	
	private ImageCache()
	{
		photosMap = new HashMap<String, SoftReference<Bitmap>>();
	}
	
	public boolean isPhotoPresentForUser(User user)
	{
		if(user==null || user.photo==null)
			return false;

		String photoDir = VKApplication.getInstance().getAvatarsDir();
		String fileName = getFileNameFromPath(user.photo);
		if(photosMap.containsKey(fileName))
			return photosMap.get(fileName).get()!=null;

		File fileOnSd = new File(photoDir,fileName);
		if(fileOnSd.length()==0)
			fileOnSd.delete();
		return fileOnSd.exists();
	}
	
	public void storeBitmapForUser(Bitmap bmp, User user, String outDir)
	{
		try {
			bmp.compress(CompressFormat.PNG, 100, new FileOutputStream(new File(outDir,getFileNameFromPath(user.photo)),false));
			photosMap.put(getFileNameFromPath(user.photo), new SoftReference<Bitmap>(bmp));
		} catch (FileNotFoundException e) {
		}
	}
	
	public boolean isPhotoPresentForUserInternal(Context context, User user)
	{
		String fileName = getFileNameFromPath(user.photo);
		if(photosMap.containsKey(fileName))
			return true;
		try {
			if(context.openFileInput(fileName) != null)
				return true;
		} catch (FileNotFoundException e) {
			return false;
		}
		File fileOnSd = new File(Environment.getExternalStorageDirectory() + context.getString(R.string.photos_cache_dir),fileName);
		return fileOnSd.exists();
	}
	
	public Bitmap getPhotoForUser(User user)
	{
		String photoDir = VKApplication.getInstance().getAvatarsDir();
		if(user==null || user.photo==null || user.uid<=0)
			return null;
		String fileName = getFileNameFromPath(user.photo);
		if(photosMap.containsKey(fileName))
			return photosMap.get(fileName).get();
		
		
		File fileOnSd = new File(photoDir,fileName);
		if(fileOnSd.exists())
		{
			Options opts = new Options();
		//	opts.inPurgeable = true;
			Bitmap bmp = BitmapFactory.decodeFile(fileOnSd.getAbsolutePath(), opts);
			if(bmp==null)
			{
				fileOnSd.delete();
				return null;
			}
			photosMap.put(fileName, new SoftReference<Bitmap>(bmp));
			return bmp;
		}
		return null;
	}
	
	
	public Bitmap getSelfSmallImage(Bitmap image)
	{
		if(selfSmallImage==null)
			selfSmallImage = ImageUtil.getScaledBitmap(image, 0.8f);
		return selfSmallImage;
	}
	private String getFileNameFromPath(String url)
	{
		return url.substring(url.lastIndexOf('/')+1, url.length());
	}
	
	public void clearCache()
	{
		Iterator<SoftReference<Bitmap>> i = photosMap.values().iterator();
		while(i.hasNext())
		{
			SoftReference<Bitmap> next = i.next();
//			next.get().recycle();
			next.clear();
		}
	}
	/**
	 * SingletonHolder is loaded on the first execution of
	 * DataProvider.getInstance() or the first access to
	 * SingletonHolder.INSTANCE, not before.
	 */
	private static class SingletonHolder {
		public static ImageCache INSTANCE;
	}

	public static ImageCache getInstance() {
		if(SingletonHolder.INSTANCE==null)
			SingletonHolder.INSTANCE = new ImageCache();
		return SingletonHolder.INSTANCE;
	}
	
	
}
