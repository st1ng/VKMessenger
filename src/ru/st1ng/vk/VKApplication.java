package ru.st1ng.vk;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ru.st1ng.vk.data.DatabaseHelper;
import ru.st1ng.vk.data.ImageCache;
import ru.st1ng.vk.data.RecordsProvider;
import ru.st1ng.vk.model.User;
import ru.st1ng.vk.model.VkAccount;
import ru.st1ng.vk.network.ErrorCode;
import ru.st1ng.vk.network.async.DownloadImagesTask;
import ru.st1ng.vk.network.async.UsersGetTask;
import ru.st1ng.vk.network.async.BasicAsyncTask.AsyncCallback;
import ru.st1ng.vk.util.FontsUtil;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Environment;
import android.util.DisplayMetrics;

public class VKApplication extends Application {

	public static final String TAG = "VKMessenger";
	private static VKApplication instance;
	private static DatabaseHelper dbHelper;
	private VkAccount currentUser;
	
	public VKApplication()
	{
		instance = this;
	}
	public static VKApplication getInstance()
	{
		return instance;
	}
		
	@Override
	public void onCreate() {
		super.onCreate();
		instance = this;		
		dbHelper = new DatabaseHelper(getApplicationContext());	
		FontsUtil.Helvetica = Typeface.createFromAsset(getAssets(), "fonts/roboto.ttf");
		FontsUtil.MyRiad = Typeface.createFromAsset(getAssets(), "fonts/myriad.ttf");
		FontsUtil.Roboto = Typeface.createFromAsset(getAssets(), "fonts/roboto.ttf");
		
	}
	
	public static DatabaseHelper getDatabase()
	{
		return dbHelper;
	}
	
	public void logOff() {
	    dbHelper.logOff();
	    setCurrentUser(null);
	}
	
	public void setCurrentUser(VkAccount user)
	{
		currentUser = user;
	}
	
	public VkAccount getCurrentUser()
	{
		if(currentUser==null)
		{
			ArrayList<VkAccount> accounts = getDatabase().getAccounts();
			if(accounts!=null && accounts.size()>0 && !accounts.get(0).token.equals(""))
			{
				currentUser = accounts.get(0);
				if(ImageCache.getInstance().isPhotoPresentForUser(currentUser))
					currentUser.photo_bitmap = ImageCache.getInstance().getPhotoForUser(currentUser);
			}
		}
		return currentUser;
	}
	
	private String photosDir = null;
	public String getAvatarsDir()
	{
		if(photosDir==null)
		{
			if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
				photosDir = Environment.getExternalStorageDirectory() + getApplicationContext().getString(R.string.photos_cache_dir);
			else
				photosDir = Environment.getDownloadCacheDirectory().getAbsolutePath();

			File oldFile = new File(Environment.getExternalStorageDirectory() + getApplicationContext().getString(R.string.photos_cache_dir_old));
			if(oldFile.exists() && oldFile.isDirectory())
			{
				for(File file : oldFile.listFiles())
					file.delete();
			}
			oldFile.delete();
		}
		return photosDir;
	}

	public String cameraDir = null;
	public String getCameraDir()
	{
		if(cameraDir==null)
		{
			if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
				cameraDir = Environment.getExternalStorageDirectory() + getApplicationContext().getString(R.string.camera_cache_dir);
			else
				cameraDir = Environment.getDownloadCacheDirectory().getAbsolutePath();
			
		}
		return cameraDir;
	}
	
	private String groupPhotosDir = null;
	public String getGroupAvatarsDir()
	{
		if(groupPhotosDir==null)
		{
			if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
				groupPhotosDir = Environment.getExternalStorageDirectory() + getApplicationContext().getString(R.string.photos_cache_dir);
			else
				groupPhotosDir = Environment.getDataDirectory() + getApplicationContext().getString(R.string.photos_cache_dir);
		}
		return groupPhotosDir;
	}
	
	private String docsDir = null;
	public String getDocsDir()
	{
		if(docsDir==null)
		{
			if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
				docsDir = Environment.getExternalStorageDirectory() + getApplicationContext().getString(R.string.docs_dir);
			else
				docsDir = Environment.getDataDirectory() + getApplicationContext().getString(R.string.docs_dir);
			if(!new File(docsDir).exists())
				new File(docsDir).mkdirs();
		}
		
		return docsDir;
	}
	
	private String avatarSize = null;
	//We don't need hi-res avatar images on mdpi and ldpi screens
	public String getAvatarSize() { 
	    if(avatarSize==null) {
	        avatarSize = getResources().getDisplayMetrics().densityDpi>=DisplayMetrics.DENSITY_HIGH ? "photo_medium_rec" : "photo_rec";
	    }
	    return avatarSize;
	}
	
	private String apiLang = null;
	public String getAPILang()
	{
		if(apiLang==null)
		{
			apiLang = getApplicationContext().getString(R.string.lang);
		}
		return apiLang;
	}

    public void runUpdateUser(final VkAccount acc)
    {
    	if(acc.photo!=null)
    	{
        	if(ImageCache.getInstance().isPhotoPresentForUser(acc))
        		acc.photo_bitmap = ImageCache.getInstance().getPhotoForUser(acc);
        	else
        		new DownloadImagesTask(new AsyncCallback<Void>() {

					@Override
					public void OnSuccess(Void str) {
		        		acc.photo_bitmap = ImageCache.getInstance().getPhotoForUser(acc);						
					}

					@Override
					public void OnError(ErrorCode errorCode) {
						
					}
				}).execute(acc);
    	}
    	new UsersGetTask(new AsyncCallback<List<User>>() {
			
			@Override
			public void OnSuccess(List<User> str) {
				if(str.size()>0)
				{
					User user = str.get(0);
					VkAccount account = acc;
					account.photo = user.photo;
					account.photo_medium = user.photo_medium;
					account.first_name = user.first_name;
					account.last_name = user.last_name;
					VKApplication.getDatabase().addOrReplaceUser(account);
					VKApplication.getInstance().setCurrentUser(account);

					new DownloadImagesTask(null).execute(account);
				}
			}
			
			@Override
			public void OnError(ErrorCode errorCode) {
				
			}
		},VKApplication.getInstance().getAPILang()).execute(acc.uid);
    }
    
	@Override
	public void onLowMemory() {
		super.onLowMemory();
	}
}
