package ru.st1ng.vk.network.async;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ReflectPermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import ru.st1ng.vk.VKApplication;
import ru.st1ng.vk.model.Message;
import ru.st1ng.vk.network.ErrorCode;
import ru.st1ng.vk.network.JsonParseException;
import ru.st1ng.vk.util.HttpUtil;
import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

/**
 * @author st1ng
 * Abstract AsyncTask.
 */
public abstract class BasicAsyncTask<T,V,U> extends AsyncTask<T,V,U> {
	public static final String CLIENT_ID = PASTE YOUR VK APP ID HERE;
	public static final String CLIENT_SECRET = PASTE YOUR VK APP SECRET HERE;
	public static final String SCOPE = "friends,messages,nohttps,offline,notify,photos,video,docs,audio";
	public static final String API_URL = "http://api.vk.com/method/";
	public static final String API_URL_HTTPS = "https://api.vk.com/method/";
	public static final String API_URL_AUTH = "https://api.vk.com/";
	public static final String USER_FIELDS = "uid,first_name,last_name,nickname,screen_name,online,last_seen,sex,has_mobile,"+ VKApplication.getInstance().getAvatarSize()+",photo_big"; 
	protected String lang;
	protected ErrorCode errorCode;
	protected int attempts = 0;
	protected AsyncCallback<U> handler;
	
	protected boolean useHttps = false;
	protected boolean useHttpsSig = false;
	@SuppressLint("NewApi")
	public BasicAsyncTask(AsyncCallback<U> callback) {
		this.handler = callback;
		
		//Hash for use in API 11+, because we want to execute Async Methods parallely
		if(Build.VERSION.SDK_INT>=11) {
			try {
				Method executor  = this.getClass().getMethod("setDefaultExecutor", Executor.class);
				executor.invoke(this, AsyncTask.THREAD_POOL_EXECUTOR);
			} catch (NoSuchMethodException e) {
			} catch (IllegalArgumentException e) {
			} catch (IllegalAccessException e) {
			} catch (InvocationTargetException e) {
			}
		}
	}
	
	protected U doInBackground(T... params) {
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
			if(!useHttps && !useHttpsSig)
				response = HttpUtil.getHttpSig(API_URL+getMethodName(), nameValuePairs,VKApplication.getInstance().getCurrentUser().secret);
			else if(useHttpsSig)
				response = HttpUtil.getHttpsSig(API_URL_HTTPS+getMethodName(), nameValuePairs,VKApplication.getInstance().getCurrentUser().secret);
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
		
	};
	
	@Override
	protected void onPostExecute(U result) {
		if(handler!=null)
		{
			if(errorCode!=ErrorCode.NoError)
			{
				if(attempts==0)
					handler.OnError(errorCode);
			}
			else
			{
				handler.OnSuccess(result);
			}		
		}
		super.onPostExecute(result);
	}
	
	@Override
	protected void onProgressUpdate(V... values) {
		if(handler!=null)
			handler.OnError(errorCode);
		super.onProgressUpdate(values);
	}
	
	public interface AsyncCallback<U>
	{
		public void OnSuccess(U str);
		public void OnError(ErrorCode errorCode);
	}

	public abstract String getMethodName();
	public abstract U parseResponse(String response) throws JsonParseException;
	public abstract void initNameValuePairs(ArrayList<NameValuePair> nameValuePairs, T... params);
}
