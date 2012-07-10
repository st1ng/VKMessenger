package ru.st1ng.vk.util;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;

public class SystemUtil {

	
	public static boolean isMyServiceRunning(Context context) {
	    ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if ("ru.st1ng.vk.data.RecordsProvider".equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}
}
