package ru.st1ng.vk.network.async;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import ru.st1ng.vk.VKApplication;
import ru.st1ng.vk.model.LongPollServer;
import ru.st1ng.vk.model.LongPollUpdate;
import ru.st1ng.vk.model.Message;
import ru.st1ng.vk.model.VkAccount;
import ru.st1ng.vk.network.ErrorCode;
import ru.st1ng.vk.network.JSONParser;
import ru.st1ng.vk.network.JsonParseException;
import ru.st1ng.vk.util.HttpUtil;
import ru.st1ng.vk.util.SettingsUtil;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;


public class LongPollTask extends BasicAsyncTask<Void, LongPollUpdate, Void> {

	LongPollHandler handler;
	private static final String PATH_SERVER = "messages.getLongPollServer";
	String token;
	private Context context;
	public LongPollTask(LongPollHandler handler, String token, Context context)
	{
		super(null);
		this.handler = handler;
		this.token = token;
		this.context = context;
	}
	
	Thread sleepThread;
	volatile long  backgroundStateEntered = 0;
	boolean firstRun = true;
	@Override
	protected Void doInBackground(Void... params) {
		errorCode = ErrorCode.NoError;
        LongPollServer server = null;

		int attempts = 0;
		while(!isCancelled())
		{
			ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("client_id", CLIENT_ID));
			nameValuePairs.add(new BasicNameValuePair("client_secret", CLIENT_SECRET));
			nameValuePairs.add(new BasicNameValuePair("access_token", token));
			String responseServer;
			try
			{
				responseServer = HttpUtil.getHttpSig(API_URL+PATH_SERVER, nameValuePairs,VKApplication.getInstance().getCurrentUser().secret);
			}
			catch (Exception e)
			{
				attempts++;
				try {
					Log.d(VKApplication.TAG,"Failed get LongPoll server. Network Unavailable. Attempt N" + attempts);
					Thread.sleep((long) (5000+Math.pow(2,attempts%10)*1000));
				} catch (InterruptedException e1) {
				}
				continue;
			}
			attempts=0;
			if(responseServer==null)
			{
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
				}
				continue;
			}
			try {
			    if(server==null)
			        server = JSONParser.parseLongPollServerResponse(responseServer);
			
			} catch (JsonParseException e) {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
				}
				continue;
			}			
			
			nameValuePairs = new ArrayList<NameValuePair>();	
			nameValuePairs.add(new BasicNameValuePair("client_id", CLIENT_ID));
			nameValuePairs.add(new BasicNameValuePair("client_secret", CLIENT_SECRET));
			nameValuePairs.add(new BasicNameValuePair("access_token", VKApplication.getInstance().getCurrentUser().token));

			
			while(!isCancelled())
			{
				long lastTs = SettingsUtil.getLastLongPollTS(context);
				if((backgroundStateEntered==0 || (System.currentTimeMillis()-backgroundStateEntered)<500000))
				{
					String url = "http://" + server.server + "?act=a_check&key=" +server.key + "&ts=" + server.ts + "&wait=25&mode=2";
					String response = HttpUtil.getHttpLongPoll(url, nameValuePairs);
					Log.d("LPOLL","Got live update response");
					if(response==null)
						break;
					try {
						LongPollUpdate update = JSONParser.parseLongPollResponse(response);
						server.ts = update.ts;
						SettingsUtil.setLastLongPollTS(context, Long.parseLong(server.ts));
						publishProgress(update);
					} catch (JsonParseException e) {
						if(e.getErrorCode()==ErrorCode.LongPollKeyNeedToBeUpdated) {
						    server=null;
                            break;
						}
					}
				}				
				else
				{
					try {
						sleepThread = Thread.currentThread();
						
						Log.d("LPOLL","Entering sleep state after " + (System.currentTimeMillis()-backgroundStateEntered)/1000 + " sec");						
						Thread.sleep(150000);
						Log.d("LPOLL","Wake up!");						
						sleepThread = null;
						
						//If we on 2.2 or more - we will use GCM instead. 
						//Also, if GCM registration failed - use short poll too
						if(Build.VERSION.SDK_INT>=8 && SettingsUtil.isGCMRegistered(context))
							continue;
						
						//On 2.1 we will use short polling
						ArrayList<NameValuePair> namePairs = new ArrayList<NameValuePair>();
						if(firstRun && lastTs>0) {
							namePairs.add(new BasicNameValuePair("ts", lastTs+""));
						}
						else {
							namePairs.add(new BasicNameValuePair("ts", server.ts));
						}
						firstRun = false;
						namePairs.add(new BasicNameValuePair("client_id", CLIENT_ID));
						namePairs.add(new BasicNameValuePair("client_secret", CLIENT_SECRET));
						if(VKApplication.getInstance().getCurrentUser()==null)
						{
							errorCode = ErrorCode.UserNotLoggedIn;
//							return null;
						}
						else
						{
							namePairs.add(new BasicNameValuePair("access_token", VKApplication.getInstance().getCurrentUser().token));
						}
						String response;
						try {
							response = HttpUtil.getHttpSig(API_URL + "messages.getLongPollHistory", namePairs,VKApplication.getInstance().getCurrentUser().secret);
						} catch (Exception e1) {
							setBackground();
							break;							
						}
						
						if(response==null)
						{
							setBackground();
							break;
						}
						LongPollUpdate update;
						try {
							update = JSONParser.parseLongPollHistory(response);
						} catch (JsonParseException e) {
							setBackground();
							break;							
						}
						
						publishProgress(update);
						for(int i = 0;i<update.updates.size();i++)
						{
							if(update.updates.get(i).type==4)
							{
								setBackground();
								break;
							}
						}

					} catch (InterruptedException e) {
					} finally {
						sleepThread = null;
					}
				}
			}			
		}
		return null;
	}

	@Override
	protected void onProgressUpdate(LongPollUpdate... values) {
		handler.OnUpdate(values[0]);
		super.onProgressUpdate(values);
	}
	@Override
	protected void onPostExecute(Void d) {
		if(errorCode!=ErrorCode.NoError)
		{
			handler.OnError(errorCode);
		}
		super.onPostExecute(d);
	}
	
	
	public interface LongPollHandler
	{
		public void OnUpdate(LongPollUpdate update);
		public void OnError(ErrorCode errorCode);
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
			Void... params) {
	}
	
	public void setForeground()
	{
		if(backgroundStateEntered==0)
			return;
		Log.d("LPOLL", "Entering foreground state");
		backgroundStateEntered = 0;
		if(sleepThread!=null)
			sleepThread.interrupt();
	}
	
	public void setBackground()
	{
		Log.d("LPOLL", "Entering background state");
		backgroundStateEntered = System.currentTimeMillis();
	}
	
}
