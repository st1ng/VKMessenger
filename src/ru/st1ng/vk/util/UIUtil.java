package ru.st1ng.vk.util;

import android.content.Context;
import android.widget.Toast;

public class UIUtil {

	
	public static void showToast(Context context, String message)
	{
		Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	}
}
