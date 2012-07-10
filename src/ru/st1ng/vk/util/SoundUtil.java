package ru.st1ng.vk.util;

import java.io.IOException;

import ru.st1ng.vk.R;
import ru.st1ng.vk.VKApplication;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources.NotFoundException;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

public class SoundUtil {

	private static MediaPlayer incomingMessage;
	public static void playIncomingMessage(Context context)
	{
		if(incomingMessage==null)
		{
			try {
				AssetFileDescriptor afd = context.getResources().openRawResourceFd(R.raw.incoming_message);
				if(afd==null)
					return;
				incomingMessage = new MediaPlayer();
				incomingMessage.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getDeclaredLength());
				afd.close();
				incomingMessage.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
				incomingMessage.prepare();
			} catch (Exception e)
			{
				return;
			}
		}
		Log.d(VKApplication.TAG, "Incoming message sound");
		incomingMessage.start();
	}
}
