package ru.st1ng.vk.network.async;

import java.io.File;
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
import ru.st1ng.vk.util.WatcherMultipartEntity.OutProgressListener;
import android.os.AsyncTask;
import android.text.InputFilter.LengthFilter;

/**
 * @author st1ng
 * AsyncTask for getting dialogs.
 * Execute params:
 * 0 - Dialog return count
 * 1 - Dialog return offset
 */

public class UploadPhotoToServer extends BasicAsyncTask<String, Integer, ServerUploadFile[]> {

	OutProgressListener listener;

	UploadAsyncCallback<ServerUploadFile[]> handle;
	public UploadPhotoToServer(UploadAsyncCallback<ServerUploadFile[]> callback) {
		super(callback);
		handle = callback;
		
		listener = new OutProgressListener() {
			long lenght;
			long count=0;
			int file = 0;
			int lastPercent = 0;
			@Override
			public void transferred(long num) {
				count=num;
				int percent = (int) ((count*100)/lenght);
				if(percent-lastPercent>5 || percent==100)
				{
					publishProgress(percent, file);
					
					lastPercent = percent;
				}
			}
			
			public void setLength(long length)
			{
				count = 0;
				lastPercent = 0;
				this.lenght = length;
				file++;
			}
		};
	}
	
	@Override
	protected ServerUploadFile[] doInBackground(String... params) {
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
		ArrayList<ServerUploadFile> files = new ArrayList<ServerUploadFile>();
		String response;
		try {
			for(int i = 1;i<params.length;i++)
			{
				listener.setLength(new File(params[i]).length());
				response = HttpUtil.uploadFile(params[0], params[i], listener);
				try {
					ServerUploadFile[] result = parseResponse(response);
					files.add(result[0]);
				} catch (JsonParseException e) {
					errorCode = e.getErrorCode();
					return null;
				}
			}
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
		return files.toArray(new ServerUploadFile[files.size()]);
	}
	
	@Override
	protected void onProgressUpdate(Integer... values) {
		if(handle!=null)
			handle.OnProgress(values[0],values[1]);
	}


	@Override
	public String getMethodName() {
		return "photos.getMessagesUploadServer";
	}

	@Override
	public ServerUploadFile[] parseResponse(String response) throws JsonParseException {
		
		return new ServerUploadFile[] { JSONParser.parseUploadedFile(response) };
	}

	@Override
	public void initNameValuePairs(ArrayList<NameValuePair> nameValuePairs,
			String... params) {
	}
	
	public interface UploadAsyncCallback<U> extends AsyncCallback<U>
	{
		public void OnProgress(int percent,int fileCount);
	}
}
