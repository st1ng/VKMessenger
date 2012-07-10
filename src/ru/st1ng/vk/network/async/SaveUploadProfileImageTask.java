package ru.st1ng.vk.network.async;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import ru.st1ng.vk.VKApplication;
import ru.st1ng.vk.data.ImageCache;
import ru.st1ng.vk.model.Message;
import ru.st1ng.vk.model.ServerUploadFile;
import ru.st1ng.vk.model.User;
import ru.st1ng.vk.model.VkAccount;
import ru.st1ng.vk.network.ErrorCode;
import ru.st1ng.vk.network.JSONParser;
import ru.st1ng.vk.network.JsonParseException;
import ru.st1ng.vk.util.HttpUtil;
import android.os.AsyncTask;

/**
 * @author st1ng
 * AsyncTask for getting dialogs.
 * Execute params:
 * 0 - Dialog return count
 * 1 - Dialog return offset
 */

public class SaveUploadProfileImageTask extends BasicAsyncTask<ServerUploadFile, Void, String> {

	public SaveUploadProfileImageTask(AsyncCallback<String> callback) {
		super(callback);
	}

	
	@Override
	protected String doInBackground(ServerUploadFile... params) {
		errorCode = ErrorCode.NoError;
		ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("client_id", CLIENT_ID));
		nameValuePairs.add(new BasicNameValuePair("client_secret", CLIENT_SECRET));
		if(VKApplication.getInstance().getCurrentUser()==null)
		{
			errorCode = ErrorCode.UserNotLoggedIn;
//			return null;
		}
		else
		{
			nameValuePairs.add(new BasicNameValuePair("access_token", VKApplication.getInstance().getCurrentUser().token));
		}
		initNameValuePairs(nameValuePairs,params);

		String response;
		try {
			if(!useHttps)
				response = HttpUtil.getHttpSigNotEncode(API_URL+getMethodName(), nameValuePairs,VKApplication.getInstance().getCurrentUser().secret);
			else
				response = HttpUtil.getHttps(API_URL_HTTPS+getMethodName(), nameValuePairs);
		} catch (Exception e1) {
			attempts++;
			errorCode = ErrorCode.NetworkUnavailable;
			
			if(attempts<3)
			{
				if(attempts==1)
					publishProgress();
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
				}
				return doInBackground(params);
			}
			
			return null;
		}
		attempts = 0;
		try {
			return parseResponse(response);
		} catch (JsonParseException e) {
			errorCode = e.getErrorCode();
			return null;
		}	
	}
	
	@Override
	public String getMethodName() {
		return "photos.saveProfilePhoto";
	}

	@Override
	public String parseResponse(String response) throws JsonParseException {
		
		return JSONParser.parseProfilePhotoSrc(response);
	}


	@Override
	public void initNameValuePairs(ArrayList<NameValuePair> nameValuePairs,
			ServerUploadFile... params) {
		ServerUploadFile param = params[0];
		nameValuePairs.add(new BasicNameValuePair("server", param.server));
		nameValuePairs.add(new BasicNameValuePair("photo", URLEncoder.encode(param.photo)));
		nameValuePairs.add(new BasicNameValuePair("hash", param.hash));
	}
}
