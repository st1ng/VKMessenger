package ru.st1ng.vk.network.async;

import java.util.ArrayList;
import java.util.EnumSet;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import ru.st1ng.vk.VKApplication;
import ru.st1ng.vk.data.ImageCache;
import ru.st1ng.vk.model.User;
import ru.st1ng.vk.model.VkAccount;
import ru.st1ng.vk.network.ErrorCode;
import ru.st1ng.vk.network.JSONParser;
import ru.st1ng.vk.network.JsonParseException;
import ru.st1ng.vk.util.HttpUtil;
import ru.st1ng.vk.util.ImageUtil;
import android.content.Context;
import android.os.AsyncTask;


public class DownloadImagesTask extends BasicAsyncTask<User, Void, Void> {

	private static final String PATH = "auth.signup";
	String avatarsPath;
	
	public DownloadImagesTask(AsyncCallback<Void> handler)
	{
		super(handler);
		this.avatarsPath = VKApplication.getInstance().getAvatarsDir();
	} 
	
	int downloaded;
	int loaded;
	@Override
	protected Void doInBackground(User... params) {
		errorCode = ErrorCode.NoError;
		downloaded = 0;
		loaded = 0;
		for(User user : params)
		{
			if(user.photo_bitmap!=null || user.photo==null)
				continue;
			if(!ImageCache.getInstance().isPhotoPresentForUser(user))
			{
				downloaded++;
				String userImageFile;
				try {
					userImageFile = HttpUtil.downloadUrlToFile(user.photo, avatarsPath);
					ImageUtil.processRoundedCornerBitmap(userImageFile, 4);
				} catch (Exception e) {
					attempts++;
					errorCode = ErrorCode.NetworkUnavailable;
					
					if(attempts<3)
					{
						if(attempts==1)
						try {
							Thread.sleep(5000);
						} catch (InterruptedException ex) {
						}
						return doInBackground(params);
					}
				}
			}
			loaded++;
			user.photo_bitmap = ImageCache.getInstance().getPhotoForUser(user);
			if(downloaded>3 || loaded>30)
			{
				publishProgress();
				downloaded = 0;
				loaded = 0;
			}
		}
		return null;
	}

	@Override
	protected void onProgressUpdate(Void... values) {
		if(handler!=null)
			handler.OnSuccess(null);
		super.onProgressUpdate(values);
	}
	@Override
	protected void onPostExecute(Void result) {
		if(handler!=null)
		{
			if(errorCode!=ErrorCode.NoError)
			{
				if(downloaded>0 || loaded>0)
					handler.OnSuccess(null);
				handler.OnError(errorCode);
			}
			else
			{
				if(downloaded>0 || loaded>0)			
					handler.OnSuccess(null);
			}		
		}
		super.onPostExecute(result);
	}

	@Override
	public String getMethodName() {
		return null;
	}

	@Override
	public Void parseResponse(String response) throws JsonParseException {
		return null;
	}

	@Override
	public void initNameValuePairs(ArrayList<NameValuePair> nameValuePairs,
			User... params) {
	}
	
	
}
