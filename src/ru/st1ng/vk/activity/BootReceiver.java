package ru.st1ng.vk.activity;

import ru.st1ng.vk.VKApplication;
import ru.st1ng.vk.data.RecordsProvider;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if(VKApplication.getInstance().getCurrentUser()!=null)
		{
			context.startService(new Intent(context, RecordsProvider.class));
		}
	}

}
