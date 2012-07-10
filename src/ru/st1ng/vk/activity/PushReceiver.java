package ru.st1ng.vk.activity;

import ru.st1ng.vk.network.ErrorCode;
import ru.st1ng.vk.network.async.BasicAsyncTask.AsyncCallback;
import ru.st1ng.vk.network.async.PushRegisterTask;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PushReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

	    if (intent.getAction().equals("com.google.android.c2dm.intent.REGISTRATION")) {
	        handleRegistration(context, intent);
	    } else if (intent.getAction().equals("com.google.android.c2dm.intent.RECEIVE")) {
	        handleMessage(context, intent);
	     }
	    Log.d("VK", "C2DM message received");
	}

	private void handleMessage(Context context, Intent intent) {
		Log.d("VK","Message received");
	}

	private void handleRegistration(Context context, Intent intent) {
	    String registration = intent.getStringExtra("registration_id"); 

	    if (intent.getStringExtra("error") != null) {
	        // Registration failed, should try again later.
	    } else if (intent.getStringExtra("unregistered") != null) {
	        // unregistration done, new messages from the authorized sender will be rejected
	    } else if (registration != null) {
	    	new PushRegisterTask(new AsyncCallback<Boolean>() {
				
				@Override
				public void OnSuccess(Boolean str) {
					
				}
				
				@Override
				public void OnError(ErrorCode errorCode) {
					
				}
			}).execute(registration);
	    }
	}
}
