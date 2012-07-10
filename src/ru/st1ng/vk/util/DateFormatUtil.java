package ru.st1ng.vk.util;

import android.content.Context;

public class DateFormatUtil {

	
	private static java.text.DateFormat dateFormat;
	public static java.text.DateFormat getDateFormat(Context context)
	{
		if(dateFormat==null)
			dateFormat = android.text.format.DateFormat.getDateFormat(context);
		return dateFormat;
	}


	private static java.text.DateFormat timeFormat;
	public static java.text.DateFormat getTimeFormat(Context context)
	{
		if(timeFormat==null)
			timeFormat = android.text.format.DateFormat.getTimeFormat(context);
		return timeFormat;
	}
}
