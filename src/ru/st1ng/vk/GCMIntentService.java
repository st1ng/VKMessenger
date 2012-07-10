package ru.st1ng.vk;


import java.util.ArrayList;
import java.util.List;

import ru.st1ng.vk.data.RecordsProvider;
import ru.st1ng.vk.model.Message;
import ru.st1ng.vk.network.ErrorCode;
import ru.st1ng.vk.network.async.BasicAsyncTask.AsyncCallback;
import ru.st1ng.vk.network.async.PushRegisterTask;
import ru.st1ng.vk.network.async.PushUnregisterTask;
import ru.st1ng.vk.util.NotificationUtil;
import ru.st1ng.vk.util.SettingsUtil;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;

import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class GCMIntentService extends GCMBaseIntentService {

	private static boolean isRegistered;
	public GCMIntentService()	{
		super("489108889244");
	}
	
	protected GCMIntentService(String senderId) {
		super(senderId);
	}


	@Override
	protected void onError(Context arg0, String arg1) {
		Log.d(VKApplication.TAG, "GCM Error");
		SettingsUtil.setGCMRegistered(this, false);
	}

	@Override
	protected void onMessage(Context arg0, Intent data) {
		Log.d(VKApplication.TAG, "GCM Message");
		Bundle extras = data.getExtras();

		int msgid;
		int uid;
		try {
			msgid = Integer.parseInt(extras.getString("msg_id"));
			uid = Integer.parseInt(extras.getString("uid"));
		} catch (Exception e) {
			return;
		}
		Message msg = new Message();
		msg.mid = msgid;
		if(uid>=2000000000)
		{
			msg.chat_id = uid-2000000000;
		}
		else
		{
			msg.uid = uid;
		}
		msg.body = extras.getString("text");
		List<Message> incoming = new ArrayList<Message>();
		incoming.add(msg);
		if(VKApplication.getInstance().getCurrentUser()!=null && !RecordsProvider.isAppRunning())
		    NotificationUtil.notifyUserIncomingMessages(this, incoming, Integer.parseInt(extras.getString("badge")));
	}

	@Override
	protected void onRegistered(final Context arg0, String token) {
		Log.d(VKApplication.TAG, "GCM Registered");
		SettingsUtil.setGCMRegistered(this, true);
		new PushRegisterTask(new AsyncCallback<Boolean>() {

			@Override
			public void OnSuccess(Boolean str) {
				
			}

			@Override
			public void OnError(ErrorCode errorCode) {
			    GCMRegistrar.unregister(arg0);
			}
		}).execute(token);
	}

	@Override
	protected void onUnregistered(Context arg0, String token) {
		Log.d(VKApplication.TAG, "GCM UnRegistered " + token);
       // SettingsUtil.setGCMRegistered(this, false);
	      new PushUnregisterTask(new AsyncCallback<Boolean>() {

	            @Override
	            public void OnSuccess(Boolean str) {
	                
	            }

	            @Override
	            public void OnError(ErrorCode errorCode) {
	            }
	        }).execute(token);
	}

	
}
