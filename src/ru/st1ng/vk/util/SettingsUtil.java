package ru.st1ng.vk.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.preference.PreferenceManager;
import android.provider.Contacts.Settings;

public class SettingsUtil {

	private static SharedPreferences settings;
	private static final String CONTACTS_IMPORTED = "imported";
	private static final String NOTIFICATIONS_ENABLED = "notify";
	private static final String NOTIFICATIONS_SOUND = "notifysound";
	private static final String NOTIFICATIONS_VIBRATE = "notifyvibrate";
	private static final String SHOW_ME_ONLINE = "online";
	private static final String LAST_SHOWN_VERSION = "lastversion";
	private static final String RUNS_COUNT = "runs";
	private static final String IS_RATED = "rated";
	private static final String FONT_SIZE = "font_size";
	private static final String LAST_TS = "last_ts";
	private static final String GCM_REGISTERED = "gcm";
	
	
	public static final int ONLINE_ALWAYS = 0;
	public static final int ONLINE_LAUNCH = 1;
	public static final int ONLINE_NEVER = 2;
	

	public static final int FONT_SMALL = 0;
	public static final int FONT_MEDIUM = 1;
	public static final int FONT_LARGE = 2;

	public static final boolean isContactsImported(Context context)
	{
		return getSettings(context).getBoolean(CONTACTS_IMPORTED, false);
	}
	
	public static final void setContactsImported(Context context)
	{
		getSettings(context).edit().putBoolean(CONTACTS_IMPORTED, true).commit();
	}

	public static final boolean isGCMRegistered(Context context)
	{
		return getSettings(context).getBoolean(GCM_REGISTERED, false);
	}
	
	public static final void setGCMRegistered(Context context, boolean value)
	{
		getSettings(context).edit().putBoolean(GCM_REGISTERED, value).commit();
	}

	public static final boolean isNotificationsEnabled(Context context)
	{
		return getSettings(context).getBoolean(NOTIFICATIONS_ENABLED, true);
	}
	
	public static final void setNotificationsEnabled(Context context,boolean value)
	{
		getSettings(context).edit().putBoolean(NOTIFICATIONS_ENABLED, value).commit();
	}

	public static final boolean isNotificationsSoundEnabled(Context context)
	{
		return getSettings(context).getBoolean(NOTIFICATIONS_SOUND, true);
	}
	
	public static final void setNotificationsSoundEnabled(Context context,boolean value)
	{
		getSettings(context).edit().putBoolean(NOTIFICATIONS_SOUND, value).commit();
	}

	public static final boolean isNotificationsVibrateEnabled(Context context)
	{
		return getSettings(context).getBoolean(NOTIFICATIONS_VIBRATE, true);
	}
	
	public static final void setNotificationsVibrateEnabled(Context context,boolean value)
	{
		getSettings(context).edit().putBoolean(NOTIFICATIONS_VIBRATE, value).commit();
	}

	public static final int getOnlineShowing(Context context)
	{
		return getSettings(context).getInt(SHOW_ME_ONLINE, ONLINE_NEVER);
	}
	
	public static final void setOnlineShowing(Context context,int value)
	{
		getSettings(context).edit().putInt(SHOW_ME_ONLINE, value).commit();
	}
	
	public static final int getFontSize(Context context)
	{
		return getSettings(context).getInt(FONT_SIZE, FONT_MEDIUM);
	}
	
	public static final void setFontSize(Context context,int value)
	{
		getSettings(context).edit().putInt(FONT_SIZE, value).commit();
	}
	
	public static final boolean needToUpdate(Context context, int newVersion, PackageInfo packInfo)
	{
		if(packInfo.versionCode < newVersion && newVersion > getSettings(context).getInt(LAST_SHOWN_VERSION, 0))
		{
			getSettings(context).edit().putInt(LAST_SHOWN_VERSION, newVersion).commit();
			return true;
		}
		return false;
	}
	
	public static final boolean runCount(Context context)
	{
		int runs = getSettings(context).getInt(RUNS_COUNT, 0);
		getSettings(context).edit().putInt(RUNS_COUNT, runs+1).commit();
		return runs>12;
	}

	public static final boolean isRated(Context context)
	{
		return getSettings(context).getBoolean(IS_RATED, false);
	}

	public static final void setRated(Context context, boolean rated)
	{
		if(!rated)
		{
			getSettings(context).edit().putInt(RUNS_COUNT, 0).commit();
		}
		getSettings(context).edit().putBoolean(IS_RATED, rated).commit();
	}

	public static final long getLastLongPollTS(Context context)
	{
		return getSettings(context).getLong(LAST_TS, -1);
	}

	public static final void setLastLongPollTS(Context context, long ts)
	{
		getSettings(context).edit().putLong(LAST_TS, ts).commit();
	}
	
	private static synchronized final SharedPreferences getSettings(Context context)
	{
		if(settings==null)
		{
			settings = PreferenceManager.getDefaultSharedPreferences(context);
		}
		return settings;
	}
	
}
