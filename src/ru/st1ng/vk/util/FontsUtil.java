package ru.st1ng.vk.util;

import ru.st1ng.vk.R;
import android.content.Context;
import android.graphics.Typeface;

public class FontsUtil {

	public static Typeface Helvetica;
	public static Typeface MyRiad;
	public static Typeface Roboto;
	
	private static float fontSize = -1;
	public static float getMessageFontSize(Context context)
	{
	    
		if(fontSize<=0)
		{
			fontSize = SettingsUtil.getFontSize(context) == SettingsUtil.FONT_SMALL ? 
						context.getResources().getDimension(R.dimen.font_size_small) :
						SettingsUtil.getFontSize(context) == SettingsUtil.FONT_MEDIUM ?
						context.getResources().getDimension(R.dimen.font_size_medium) :
						context.getResources().getDimension(R.dimen.font_size_large);
		}
		return fontSize;
	}
	
	   private static float scaleFactor = -1;
	    public static float getMessageFontScaleFactor(Context context)
	    {
	        
	        if(scaleFactor<=0)
	        {
	            scaleFactor = SettingsUtil.getFontSize(context) == SettingsUtil.FONT_SMALL ? 
	                        0.7f :
	                        SettingsUtil.getFontSize(context) == SettingsUtil.FONT_MEDIUM ?
	                        1.0f :
	                        1.3f;
	        }
	        return scaleFactor;
	    }

	public static void updateFontSize()
	{
		fontSize = -1;
		scaleFactor = -1;
	}
}
