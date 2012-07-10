package ru.st1ng.vk.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ru.st1ng.vk.R;
import ru.st1ng.vk.VKApplication;
import ru.st1ng.vk.activity.ConversationActivity;
import ru.st1ng.vk.activity.TabhostActivity;
import ru.st1ng.vk.data.ImageCache;
import ru.st1ng.vk.model.Message;
import ru.st1ng.vk.model.User;
import ru.st1ng.vk.network.ErrorCode;
import ru.st1ng.vk.network.async.BasicAsyncTask.AsyncCallback;
import ru.st1ng.vk.network.async.UsersGetTask;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;
import android.widget.RemoteViews.RemoteView;

@SuppressLint("NewApi")
public class NotificationUtil {

	private static final int MESSAGE_ID = 1;
	private static List<Message> incoming = new ArrayList<Message>();
	public static void notifyUserIncomingMessages(final Context context, final List<Message> incomingMessages, final int unreadCount)
	{
		incoming.addAll(incomingMessages);
		if(!SettingsUtil.isNotificationsEnabled(context))
			return;
		final ArrayList<Message> notifyMessages = new ArrayList<Message>();
		ArrayList<Integer> missedUsers = new ArrayList<Integer>();
		final Set<Integer> usersCount = new HashSet<Integer>();

		for(int i =0;i<incoming.size();i++)
		{
			Message msg = incoming.get(i);
			if(msg.read_state || msg.out)
				continue;
			
			if(msg.user==null)
			{
				missedUsers.add(msg.uid);

			}
			notifyMessages.add(msg);
			usersCount.add(msg.getId());
		}
		
		if(missedUsers.size()>0)
		{
			UsersGetTask usersTask = new UsersGetTask(new AsyncCallback<List<User>>() {
				
				@Override
				public void OnSuccess(final List<User> str) {
					for(Message message : notifyMessages)
					{
						for(User user : str)
						{
							if(message.uid==user.uid)
							{
								message.user = user;
								break;
							}							
						}
					}
					
					if(allPhotoPresents(str))
					{
						if(usersCount.size()>1)
						{
							String names = "";
							Bitmap bmp = null;
							for(Message message : notifyMessages)
							{
								if(!names.contains(message.user.first_name))
									names+=(message.user.first_name+",");
								if(message.user.photo_bitmap!=null)
									bmp = message.user.photo_bitmap;
							}
							if(names.length()>1)
								names = names.substring(0, names.length()-1);
							notifyUserIncomingMessage(bmp, context.getString(R.string.new_messages) +" (" + notifyMessages.size() +")", names, 0, true, unreadCount);				
						}
						else if(notifyMessages.size()>0)
						{
								Message message = notifyMessages.get(notifyMessages.size()-1);
								notifyUserIncomingMessage(message.user.photo_bitmap, message.user.first_name + " " + message.user.last_name, message.body, message.getId(), false, unreadCount);
						}
					}
					else
					{
					    new Thread(new Runnable() {
                            
                            @Override
                            public void run() {
                                Bitmap icon = null;
                                try { 
                                    icon = BitmapFactory.decodeStream(HttpUtil.getInputStream(str.get(str.size()-1).photo));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                Bitmap bmp = icon;
                                if(usersCount.size()>1)
                                {
                                    String names = "";
                                    for(Message message : notifyMessages)
                                    {
                                        if(!names.contains(message.user.first_name))
                                            names+=(message.user.first_name+",");
                                        if(message.user.photo_bitmap!=null)
                                            bmp = message.user.photo_bitmap;
                                    }
                                    if(names.length()>1)
                                        names = names.substring(0, names.length()-1);
                                    notifyUserIncomingMessage(bmp, context.getString(R.string.new_messages) +" (" + notifyMessages.size() +")", names, 0, true, unreadCount);               
                                }
                                else if(notifyMessages.size()>0)
                                {
                                        Message message = notifyMessages.get(notifyMessages.size()-1);
                                        notifyUserIncomingMessage(bmp, message.user.first_name + " " + message.user.last_name, message.body, message.getId(), false, unreadCount);
                                }
                                
                            }
                        }).start();

					}
				}
				
				@Override
				public void OnError(ErrorCode errorCode) {

				}

			}, VKApplication.getInstance().getAPILang());
			if(Build.VERSION.SDK_INT>=11)
				usersTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, missedUsers.toArray(new Integer[missedUsers.size()]));
			else
				usersTask.execute(missedUsers.toArray(new Integer[missedUsers.size()]));
		}
		else
		{
			if(usersCount.size()>1)
			{
				String names = "";
				Bitmap bmp = null;
				for(Message message : notifyMessages)
				{
					if(!names.contains(message.user.first_name))
						names+=(message.user.first_name+",");
					if(message.user.photo_bitmap!=null)
						bmp = message.user.photo_bitmap;
				}
				if(names.length()>1)
					names = names.substring(0, names.length()-1);
				notifyUserIncomingMessage(bmp, context.getString(R.string.new_messages) +" (" + notifyMessages.size() +")", names, 0, true, unreadCount);				
			}
			else
			{
				Message message;
				if(notifyMessages.size()>0)
				{
					message = notifyMessages.get(notifyMessages.size()-1);
					notifyUserIncomingMessage(message.user.photo_bitmap, message.user.first_name + " " + message.user.last_name, message.body, message.getId(), false, unreadCount);
				}
			}
		}
	}
	
	private static long lastNotifiedTime = 0;
	public static void notifyUserIncomingMessage(Bitmap icon, String title, String text, int userId, boolean severalMessages, int unreadCount)
	{
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) VKApplication.getInstance().getSystemService(ns);
//		RemoteViews remoteView = new RemoteViews(VKApplication.getInstance().getPackageName(), R.layout.widget_custom_notification);
//		if(icon!=null)
//			remoteView.setBitmap(R.id.icon, "setImageBitmap", icon);
//		remoteView.setTextViewText(R.id.title, title);
//		remoteView.setTextViewText(R.id.text, text);
		Intent notificationIntent;
		notificationIntent = new Intent(VKApplication.getInstance(), TabhostActivity.class);
		if(!severalMessages)
			notificationIntent.putExtra(ConversationActivity.EXTRA_USERID, userId);
		else
			notificationIntent.putExtra(ConversationActivity.EXTRA_USERID, 0);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent contentIntent = PendingIntent.getActivity(VKApplication.getInstance(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		NotificationCompat.Builder notify = new NotificationCompat.Builder(VKApplication.getInstance());
		
		int defaults = 0;
		if(System.currentTimeMillis()-lastNotifiedTime>5000) {
			if (SettingsUtil.isNotificationsSoundEnabled(VKApplication.getInstance()))
				defaults|=Notification.DEFAULT_SOUND;
			if (SettingsUtil.isNotificationsVibrateEnabled(VKApplication.getInstance()))
				defaults = Notification.DEFAULT_VIBRATE;
		}
		
		notify = notify.setAutoCancel(true)
//				.setContent(remoteView)
				.setContentText(text)
				.setContentTitle(title)
				.setContentInfo(""+unreadCount)
				.setTicker(VKApplication.getInstance().getString(R.string.message_received))
				.setSmallIcon(R.drawable.im_notify)
				.setLargeIcon(icon)
				.setWhen(System.currentTimeMillis())
				.setDefaults(defaults)
				.setContentIntent(contentIntent)
				.setNumber(unreadCount);

		Notification not = notify.getNotification();
		mNotificationManager.notify(MESSAGE_ID, not);
	}
	
	public static void clearNotifications()
	{
		NotificationManager mNotificationManager = (NotificationManager) VKApplication.getInstance().getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancelAll();
		incoming.clear();
	}
	
	private static boolean allPhotoPresents(List<User> users) {
		for(User user : users) {
			if(!ImageCache.getInstance().isPhotoPresentForUser(user))
				return false;
			else
				user.photo_bitmap = ImageCache.getInstance().getPhotoForUser(user);
		}
		return true;
	}
}
