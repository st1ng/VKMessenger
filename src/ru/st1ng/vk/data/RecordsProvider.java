package ru.st1ng.vk.data;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import ru.st1ng.vk.BuildConfig;
import ru.st1ng.vk.VKApplication;
import ru.st1ng.vk.model.ContactName;
import ru.st1ng.vk.model.LongPollUpdate;
import ru.st1ng.vk.model.Message;
import ru.st1ng.vk.model.User;
import ru.st1ng.vk.network.ErrorCode;
import ru.st1ng.vk.network.LongPollManager;
import ru.st1ng.vk.network.LongPollManager.LongPollWatcher;
import ru.st1ng.vk.network.async.BasicAsyncTask.AsyncCallback;
import ru.st1ng.vk.network.async.DialogsGetTask;
import ru.st1ng.vk.network.async.FriendsGetTask;
import ru.st1ng.vk.network.async.GetRequestsTask;
import ru.st1ng.vk.network.async.GetUsersByPhonesTask;
import ru.st1ng.vk.network.async.MessagesByIdTask;
import ru.st1ng.vk.network.async.MessagesDeleteDialogTask;
import ru.st1ng.vk.network.async.MessagesDeleteTask;
import ru.st1ng.vk.network.async.MessagesGetTask;
import ru.st1ng.vk.network.async.SetOnlineTask;
import ru.st1ng.vk.network.async.UsersGetTask;
import ru.st1ng.vk.util.NotificationUtil;
import ru.st1ng.vk.util.SettingsUtil;
import ru.st1ng.vk.util.SoundUtil;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.util.Log;

import com.google.android.gcm.GCMRegistrar;

@SuppressLint("UseSparseArrays")
public class RecordsProvider extends Service {
	
	private final LocalBinder binder = new LocalBinder();
	
	public class LocalBinder extends Binder {
		public RecordsProvider getService() {
			return RecordsProvider.this;
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	private volatile ErrorCode lastError = ErrorCode.NoError;

	public ErrorCode getLastError()
	{
		return lastError;
	}
	
	private List<Message> dialogList;
	private List<Message> incomingMessages;
	private HashMap<Integer, Message> dialogMap;
	private HashMap<Long, Message> messagesMap;
	private HashMap<Integer, User> usersMap;
	private HashMap<Integer, List<Message>> userMessagesMap;
	private List<ContactName> contactsList;
	private List<User> requestsList;
	private HashMap<String, ContactName> hashPhones;
	ArrayList<User> friends;
	ArrayList<User> allUsers;

	DatabaseHelper dbHelper;

	LongPollManager longpollManager;
	
	private ArrayList<RecordsWatcher> watchers;

	private static final Handler appRunningHandler = new Handler();
	private static boolean appRunning = false;
    public static boolean isAppRunning() {
        return appRunning;
    }

	@Override
	public void onCreate() {
		dialogList = Collections.synchronizedList(new ArrayList<Message>());
		incomingMessages = new ArrayList<Message>();
		dialogMap = new HashMap<Integer, Message>();
		messagesMap = new HashMap<Long, Message>();
		usersMap = new HashMap<Integer, User>();
		userMessagesMap = new HashMap<Integer, List<Message>>();
		dbHelper = VKApplication.getDatabase();
		watchers = new ArrayList<RecordsWatcher>();
		longpollManager = LongPollManager.getInstance(this);
		longpollManager.addWatcher(longpollWatcher);
		populateRecordsFromDb();
		longpollManager.start();		
		super.onCreate();
		setOnline();
		if(SettingsUtil.isNotificationsEnabled(this))
		    registerGCM();
		else
		    unregisterGCM();
	}

	@Override
	public void onDestroy() {
		clearCache();
		super.onDestroy();
	}
	
	@Override
	public void onLowMemory() {
		Iterator<User> i = usersMap.values().iterator();
		while(i.hasNext())
			i.next().photo_bitmap = null;
	//	ImageCache.getInstance().clearCache();
		super.onLowMemory();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return Service.START_STICKY;
	}
	
	public interface RecordsWatcher {
		public void OnChangedRecords(boolean needInvalidate);
		public void OnError(ErrorCode errorCode);
	}

	public void addWatcher(RecordsWatcher watcher) {
		if(watcher==null)
			return;
		for(RecordsWatcher watch : watchers)
		{
			if(watch.equals(watcher))
				return;
		}
		watchers.add(watcher);
		if(watchers.size()>0) {
			longpollManager.setForeground();
			appRunningHandler.postDelayed(new Runnable() {

			    @Override
			    public void run() {
    			    if(watchers.size()>0)
    			        appRunning = true;
    			    }
			    }, 300);
		}
		setOnline();
	}

	public void removeWatcher(RecordsWatcher watcher) {
		watchers.remove(watcher);
		if(watchers.size()==0) {
			longpollManager.setBackground();
            appRunningHandler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    if(watchers.size()==0)
                        appRunning = false;
                    }
                }, 300);
		}
	}
	
	private void notifyWatchersError(ErrorCode error)
	{
		for (RecordsWatcher watcher : watchers) {
			watcher.OnError(error);
		}		
	}
	
	private void notifyWatchers() {
		notifyWatchers(true);
	}

	private void notifyWatchers(boolean needInvalidate) {
		if(BuildConfig.DEBUG)
			Log.d("VKPERF","Notify data changed");
		for (RecordsWatcher watcher : watchers) {
			watcher.OnChangedRecords(needInvalidate);
		}
	}

	private LongPollWatcher longpollWatcher = new LongPollWatcher() {
		
		@Override
		public void OnUpdate(LongPollUpdate updates) {
			boolean changed = false;
			ArrayList<Message> addedMessages = new ArrayList<Message>();
			for(LongPollUpdate.Update update : updates.updates)
			{			
				if(update.type==1)
				{
					if(messagesMap.containsKey(update.id))
					{
						Message msg = messagesMap.get(update.id);
						msg.read_state = (update.flags&1)==0;
						//msg.out = (update.flags&2)==1;
						changed = true;
					}
					
				}
				else if(update.type==2)
				{
					if(messagesMap.containsKey(update.id))
					{
						Message msg = messagesMap.get(update.id);
						msg.read_state = (update.flags&1)==0;
						//msg.out = (update.flags&2)>0;
						changed = true;
					}					
				}
				else if(update.type==3)
				{
					if(messagesMap.containsKey(update.id))
					{
						Message msg = messagesMap.get(update.id);
						msg.read_state = (update.flags&1)==0;
						//msg.out = (update.flags&2)==0;
						changed = true;
					}
				}
				else if(update.type==8)
				{
					User user = getUserById((int) -update.id,false);
					if(user!=null)
					{
						changed = true;
						user.online = true;
					}
				}
				else if(update.type==9)
				{
					User user = getUserById((int) -update.id,false);
					if(user!=null)
					{
						changed = true;
						user.online = false;
					}
				}
				else if(update.type==61)
				{
					User user = getUserById((int) update.id,false);
					if(user!=null)
					{
						changed = true;
						user.writing = true;
					}
				}
				else if(update.type==4)
				{
					final Message msg = new Message();
					msg.mid = update.id;
					if(update.from_id>=2000000000)
					{
						msg.chat_id = update.from_id-2000000000;
						msg.uid = update.from_in_chat;
					}
					else
					{
						msg.uid = update.from_id;
					}
					msg.body = update.text.replace("<br>", "\n");
					msg.date = update.timestamp;
					msg.title = update.title;
					msg.read_state = (update.flags&1)==0;
					msg.out = (update.flags&2)>0;
					if(msg.uid==VKApplication.getInstance().getCurrentUser().uid)
						msg.out = true;
					if(update.attachments!=null)
						msg.attachments = update.attachments;
					if(update.haveFwd)
						msg.fwd_messages = new ArrayList<Message>();
					addedMessages.add(msg);
					changed = true;
				}
			}
			
			if(addedMessages.size()>0)
			{
				incomingMessages.addAll(addedMessages);
				notifyUserIncomingMessage();
				addDialogs(addedMessages, true);
			}
			ArrayList<Integer> messagesNeedUpdate = new ArrayList<Integer>();
			for(Message message : addedMessages)
			{
				if(userMessagesMap.containsKey(message.getId()))
				{
					if(message.attachments!=null || message.fwd_messages!=null)
						messagesNeedUpdate.add((int) message.mid);
					if(message.out || message.uid==VKApplication.getInstance().getCurrentUser().uid)
						if(addSentMessage(message))
							continue;
					addMessage(message, message.getId());
					sortMessages(message.getId());
				}
				
			}
			if(messagesNeedUpdate.size()>0)
			{
				new MessagesByIdTask(new AsyncCallback<List<Message>>() {

					@Override
					public void OnSuccess(List<Message> str) {
						
						if(addMessages(str,0,true))
							notifyWatchers();
						populateMessageUsers(str, true);
					}

					@Override
					public void OnError(ErrorCode errorCode) {
					}
				}).execute(messagesNeedUpdate.toArray(new Integer[0]));
			}
			if(changed)
				notifyWatchers();
			lastError = ErrorCode.NoError;

		}
	};
	
	public void populateRecordsFromDb() {
		new Thread(new Runnable() {

			@Override
			public void run() {

				List<User> users = dbHelper.getUsers(VKApplication
						.getInstance().getCurrentUser().uid);
				final List<Message> dialogs = dbHelper.getDialogs(VKApplication
						.getInstance().getCurrentUser().uid, 20);
				hashPhones = dbHelper.getContacts(VKApplication
						.getInstance().getCurrentUser().uid);
/*				for (User user : users) {
					if (ImageCache.getInstance().isPhotoPresentForUser(user, VKApplication.getInstance().getAvatarsDir())) {
						user.photo_bitmap = ImageCache.getInstance()
								.getPhotoForUser(user,VKApplication.getInstance().getAvatarsDir());
					}
				}
*/
				synchronized (RecordsProvider.this) {
					synchronized (usersMap) {						
						for (User user : users) {
							usersMap.put(user.uid, user);
						}
					}
					addDialogs(dialogs, false);
					notifyWatchers();
				}
				new Thread(new Runnable() {
					
					@Override
					public void run() {
						populateMessageUsers(dialogs,false);
						notifyWatchers();
					}
				}).start();

			}
		}).start();
	}
	
	private void populateDialogUsers() {
		synchronized (dialogList) {			
			for (Message dialog : dialogList) {
				dialog.user = getUserById(dialog.uid,false);
				if(dialog.chat_id>0)
				{
					dialog.chat_users = new ArrayList<User>();
					if(dialog.chat_active==null)
						continue;
					String[] users = dialog.chat_active.split(",");
					for(String user : users)
						dialog.chat_users.add(getUserById(Integer.parseInt(user), false));
				}
			}
		}
	}
	
	public void performFriendsUpdate()
	{
		getFriendsTask().execute();
	}
	
	public void performRequestsUpdate()
	{
		new GetRequestsTask(new AsyncCallback<List<User>>() {

			@Override
			public void OnSuccess(List<User> str) {
				getRequestList().clear();
				
				if(str==null)
					return;
				getRequestList().addAll(str);
				addUsers(str);
				notifyWatchers();
			}

			@Override
			public void OnError(ErrorCode errorCode) {
			}
		}, VKApplication.getInstance().getAPILang()).execute();
	}


	
	public void performDialogsUpdate(final int count, final int from, final Activity context) {
			
		Log.d("VK", "Get more dialogs");
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				final List<Message> dialogs = dbHelper.getDialogs(VKApplication.getInstance().getCurrentUser().uid, count+from);
				final ArrayList<Message> missedDialogs = new ArrayList<Message>();
				for(Message message : dialogs)
				{
					if(dialogMap.containsKey(message.uid) && messagesMap.containsKey(message.mid) && dialogMap.get(message.uid).read_state==message.read_state)
						continue;
					missedDialogs.add(message);
				}
				context.runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						synchronized (RecordsProvider.this) {					
							addDialogs(missedDialogs,false);
							//notifyWatchers();
						}
					}
				});
				new Thread(new Runnable() {
					
					@Override
					public void run() {
						populateMessageUsers(missedDialogs,false);
						notifyWatchers();
					}
				}).start();		
			}
		}).start();
		 
		new DialogsGetTask(new AsyncCallback<List<Message>>() {
			
			@Override
			public void OnSuccess(final List<Message> result) {
				synchronized (RecordsProvider.this) {							
					if (addDialogs(result,true)) {
						notifyWatchers();
					} else {
						notifyWatchers(false);
					}
				}
				
				new Thread(new Runnable() {
					
					@Override
					public void run() {
						populateMessageUsers(result, true);
				//		notifyWatchers();
					}
				}).start();
				lastError = ErrorCode.NoError;
			}

			@Override
			public void OnError(ErrorCode errorCode) {
				lastError = errorCode;
				notifyWatchersError(errorCode);
			}
		}).execute(count, from);

	}

	public List<Message> getMessagesForDialog(int id, int offset,int syncedcount, Activity activity, int totalCount)
	{
		if(!userMessagesMap.containsKey(id))
			userMessagesMap.put(id, new ArrayList<Message>());
		populateMessages(id, offset, syncedcount,activity,totalCount);
		return userMessagesMap.get(id);
	}
	
    private void populateMessages(final int dialogid,final int offset,final int count, final Activity activity, int totalCount) {
        populateMessages(dialogid, offset, count, activity, false, totalCount);
    }
    
	private void populateMessages(final int dialogid,final int offset,final int count, final Activity activity, boolean loadOnlyWeb, final int totalCount)
	{
	    if(BuildConfig.DEBUG)
	        Log.d(VKApplication.TAG, "Get more messages!");
	    if(!loadOnlyWeb)
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				final List<Message> messages = dbHelper.getMessages(VKApplication.getInstance().getCurrentUser().uid, dialogid, totalCount+count);
				activity.runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						synchronized (RecordsProvider.this) {					
							addMessages(messages,dialogid,false);
							notifyWatchers();
						}
					}
				});
			}
		}).start();
		new MessagesGetTask(new AsyncCallback<List<Message>>() {

			@Override
			public void OnSuccess(final List<Message> result) {
				addMessages(result, dialogid, true);
				new Thread(new Runnable() {
					
					@Override
					public void run() {
					  //TODO: Make this good!
//					    if(isRecordsLeft(result, dialogid)) {
//					        populateMessages(dialogid, offset+count, count, activity, true);
//					    }
						populateMessageUsers(result, true);
						notifyWatchers();
					}
				}).start();
			}
			
			@Override
			public void OnError(ErrorCode errorCode) {
			    int error = errorCode.getStringResource();
			}
		}).execute(count,offset,dialogid);
	}
	
	boolean isRecordsLeft(List<Message> messagesFromServer, int dialogId) {
	    if(userMessagesMap==null || !userMessagesMap.containsKey(dialogId))
	        return false;
	    long minServer = Long.MAX_VALUE;
	    for(int i = 0;i<messagesFromServer.size();i++)
	        if(messagesFromServer.get(i).date<minServer)
	            minServer = messagesFromServer.get(i).date;
	    
	    List<Message> messages =   userMessagesMap.get(dialogId);
	    for(int i = 0;i<messages.size();i++) {
	        return messages.get(i).date<minServer;
	    }
	    return false;
	}
	
	private UsersGetTask getUserTask() {

		return new UsersGetTask(new AsyncCallback<List<User>>() {

			@Override
			public void OnSuccess(final List<User> result) {
				new Thread(new Runnable() {
					
					@Override
					public void run() {
						Log.d("VK", "Get users " + result.size());
						if (addUsers(result)) {
							populateDialogUsers();
							notifyWatchers();
						} else {
							populateDialogUsers();
							notifyWatchers();
						}
					//	downloadAvatarsTask().execute(result.toArray(new User[result.size()]));
					}
				}).start();
			}

			@Override
			public void OnError(ErrorCode errorCode) {
				// UIUtil.showToast(MessagesActivity.this,
				// getString(errorCode.getStringResource()));
			}
		}, VKApplication.getInstance().getAPILang());
	}
	
	private FriendsGetTask getFriendsTask() {

		return new FriendsGetTask(new AsyncCallback<List<User>>() {

			@Override
			public void OnSuccess(final List<User> result) {
				new Thread(new Runnable() {
					
					@Override
					public void run() {
						Log.d("VK", "Get users");
						if (addUsers(result)) {
							setFriends(result);
							notifyWatchers();
						} else {
							setFriends(result);
							notifyWatchers();
						}
					}
				}).start();
			}

			@Override
			public void OnError(ErrorCode errorCode) {
			}
		}, VKApplication.getInstance().getAPILang());
	}

	public boolean addOrReplaceChat(Message dialog) {
	    if(addDialog(dialog))
	        sortDialogs();
		populateDialogUsers();
		return true;
	}

	private boolean addDialog(Message dialog) {
		boolean changed = false;
		int id = dialog.chat_id>0 ? -dialog.chat_id : dialog.uid;
		if (dialogMap.containsKey(id)) {
	        Message oldDialog = dialogMap.get(id);
			if (oldDialog.mid < dialog.mid || oldDialog.read_state!=dialog.read_state || dialog.attachments!=null || dialog.chat_active!=null || dialog.fwd_messages!=null) {
				dialogList.remove(oldDialog);
				changed = true;
				dialogList.add(dialog);
				dialogMap.put(id, dialog);
				messagesMap.put(dialog.mid, dialog);
				if (usersMap.containsKey(id)) {
					dialog.user = usersMap.get(id);
				}
			}
			return changed;
		}
		dialogList.add(dialog);
		dialogMap.put(id, dialog);
		messagesMap.put(dialog.mid, dialog);
		return true;
	}

	private boolean addDialogs(final List<Message> dialogs,boolean toDb) {
		if(BuildConfig.DEBUG)
			Log.d("VKPERF","Add dialogs");
			
		boolean anythingChanded = false;
//		synchronized (this) {
		int size = dialogs.size();
		for(int i = 0;i<size;i++)
		{
				if (addDialog(dialogs.get(i)))
					anythingChanded = true;
			}
			if(anythingChanded)
			{
				sortDialogs();
			}
	//	}
		if(toDb)
		{
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					dbHelper.updateDialogs(dialogs, VKApplication.getInstance().getCurrentUser().uid);
				}
			}).start();
		}
		return anythingChanded;
	}
	
	
	public void addPendingMessage(Message message)
	{
		if(!userMessagesMap.containsKey(message.getId()))
			userMessagesMap.put(message.getId(), new ArrayList<Message>());
		List<Message> messages = userMessagesMap.get(message.getId());
		synchronized (messages) {			
			messages.add(message);
		}
	}

   public void addNetworkErrorMessage(int uid, String guid)
    {
       if(guid==null)
           return;
        List<Message> messages = userMessagesMap.get(uid);
        for(int i = 0;i<messages.size();i++) {
            if(messages.get(i).guid==null)
                continue;
            if(messages.get(i).guid.equals(guid)) {
                messages.get(i).networkError = true;
                break;
            }
        }
        notifyWatchers();
    }

	public boolean addSentMessage(Message message)
	{
		if(!userMessagesMap.containsKey(message.getId()))
			userMessagesMap.put(message.getId(), new ArrayList<Message>());
		List<Message> messages = userMessagesMap.get(message.getId());

		synchronized (messages) {			
			int size = messages.size();
			for(int i = size-1;i>0;i--)
			{
				Message mess = messages.get(i);
				if(!mess.pendingSend)
					continue;
				if(message.body.equals(mess.body))
				{
					if(message.date>mess.date && message.date!=0)
						mess.date = message.date;
					mess.mid = message.mid;
					mess.sent = true;
					messagesMap.put(mess.mid, mess);
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean addMessage(Message dialog, int conversationId) {
		boolean changed = false;
		if(!userMessagesMap.containsKey(conversationId))
			userMessagesMap.put(conversationId, new ArrayList<Message>());
		List<Message> messages = userMessagesMap.get(conversationId);
			if(messagesMap.containsKey(dialog.mid))
			{
				Message old = messagesMap.get(dialog.mid);
				if(dialog.fromDb)
				    return false;
				synchronized (messages) {					
					if(!isContainsMessage(messages, dialog))
						messages.add(dialog);
				}
				if(old.deleted!=dialog.deleted || old.read_state!=dialog.read_state || (old.attachments!=null) || (old.fwd_messages!=null) || dialog.attachments!=null || dialog.fwd_messages!=null)
				{
					changed = true;
					old.read_state = dialog.read_state;					
					old.attachments = dialog.attachments;
					old.fwd_messages = dialog.fwd_messages;
					old.deleted = dialog.deleted;
				}
				if(!dialog.fromDb)
				    old.fromDb=false;
			}
			else
			{
				changed = true;
				synchronized (messages) {					
					messagesMap.put(dialog.mid, dialog);
					messages.add(dialog);
				}
			}
			return changed;
	}
 
	private boolean isContainsMessage(List<Message> messages, Message message) {
	    int size = messages.size();
	    for(int i = 0;i<size;i++) {
	        if(messages.get(i).mid==message.mid)
	            return true;
	    }
	    return false;
	}
	private boolean addMessages(final List<Message> messages, int conversationId, boolean toDb) {
		boolean anythingChanded = false;
		boolean useInternalId = (conversationId == 0);
		synchronized (this) {
			for (Message message : messages) {
			    if(useInternalId)
			        conversationId = message.getId();
				if (addMessage(message,conversationId))
					anythingChanded = true;
			}
		}
		if(messages.size()>0)
			sortMessages(conversationId);
		if(toDb)
		{
		    final int convId = conversationId;
			new Thread(new Runnable() {
				
				@Override
				public void run() {
				    
					dbHelper.updateMessages(messages, VKApplication.getInstance().getCurrentUser().uid,convId);
				}
			}).start();
		}
		return anythingChanded;
	}
	 
   public Message getPendingMessage(final int dialogId, final String guid) {
        for(Message message : userMessagesMap.get(dialogId))
        {
            if(message.guid==null)
                continue;
            if(message.guid.equals(guid)) {
                return message;
            }
        }
        return null;
    }
    
	public void deletePendingMessage(final int dialogId, final String guid) {
        for(Message message : userMessagesMap.get(dialogId))
        {
            if(message.guid==null)
                continue;
            if(message.guid.equals(guid)) {
                userMessagesMap.get(dialogId).remove(message);
                break;
            }
        }
        notifyWatchers();
	}
	
	public void deleteMessages(final int dialogId, final List<Integer> messages) {
		new MessagesDeleteTask(new AsyncCallback<Boolean>() {

			@Override
			public void OnSuccess(Boolean str) {
				List<Message> deleteMessages = new ArrayList<Message>();
				for(Message message : userMessagesMap.get(dialogId))
				{
					for(Integer i : messages)
					{
						if(message.mid==i)
						{
							message.deleted = true;
							deleteMessages.add(message);
							break;
						}
					}
				}
				userMessagesMap.get(dialogId).removeAll(deleteMessages);
//				addMessages(changedMessages, true);
				notifyWatchers();
				new Thread(new Runnable() {
					
					@Override
					public void run() {
						dbHelper.deleteMessagesById(messages);
					}
				}).start();
				ArrayList<Message> deleteDialogs = new ArrayList<Message>();
				for(final Message dialog : dialogList)
				{
					if(dialog.getId()!=dialogId)
						continue;
					for(Integer message : messages)
					{
						if(message==dialog.mid)
						{
							new Thread(new Runnable() {
								
								@Override
								public void run() {
									dbHelper.deleteDialogsById(dialog.getId(),false);
								}
							}).start();
							deleteDialogs.add(dialog);
							break;
						}
					}
				}
				dialogList.removeAll(deleteDialogs);
				for(Message d : deleteDialogs)
					dialogMap.remove(d.uid);
				notifyWatchersError(ErrorCode.MessagesSuccessfullyDeleted);
			}

			@Override
			public void OnError(ErrorCode errorCode) {
				notifyWatchersError(errorCode);
			}
		}).execute(messages.toArray(new Integer[messages.size()]));
	}
	
	public void deleteDialogs(final int dialogId)	{
		new MessagesDeleteDialogTask(new AsyncCallback<Boolean>() {

			@Override
			public void OnSuccess(Boolean str) {
				for(Message message : dialogList)
				{
					if(message.getId()==dialogId)
					{
						dialogList.remove(message);
						break;
					}
				}
				if(userMessagesMap.containsKey(dialogId))
				    userMessagesMap.remove(dialogId);
				dbHelper.deleteDialogsById(dialogId, true);
				notifyWatchersError(ErrorCode.MessagesSuccessfullyDeleted);

			}

			@Override
			public void OnError(ErrorCode errorCode) {
				
			}
		}).execute(dialogId);
	}
	
	   public void deleteUser(final int userId)   {
	       if(usersMap.containsKey(userId))
	           usersMap.remove(userId);
	       dbHelper.deleteUserById(userId, true);
	    }

	public void populateMessageUsers(final List<Message> dialogs, boolean networkUpdate)
	{
		ArrayList<Integer> missedUsers = new ArrayList<Integer>();
		for(Message dialog : dialogs)
		{
			int id = dialog.chat_id>0 ? -dialog.chat_id : dialog.uid;
			if(id>0 || id <-200000000)
			{
				missedUsers.add(id);
			}
			else
			{
				if(dialog.chat_active!=null)
				{
					String[] userIds = dialog.chat_active.split(",");
					for(String userId : userIds)
					{
						try
						{ 
							int partid = Integer.parseInt(userId);
							missedUsers.add(partid);
							if(usersMap.containsKey(partid))
							{
								if(dialog.chat_users==null)
									dialog.chat_users = new ArrayList<User>();
								dialog.chat_users.add(usersMap.get(partid));
							}
						} catch (Exception e) {}
					}
				}
			}
			if (usersMap.containsKey(id)) {
				dialog.user = usersMap.get(id);
			}
			if(dialog.fwd_messages!=null)
			{
				ArrayList<User> dummyUsers = new ArrayList<User>();
				for(int i= 0;i<dialog.fwd_messages.size();i++)
				{
					if(usersMap.containsKey(dialog.fwd_messages.get(i).uid))
					{
						dialog.fwd_messages.get(i).user = usersMap.get(dialog.fwd_messages.get(i).uid);
						if(dialog.fwd_messages.get(i).user.photo_bitmap==null && ImageCache.getInstance().isPhotoPresentForUser(dialog.fwd_messages.get(i).user))
						{
							dialog.fwd_messages.get(i).user.photo_bitmap = ImageCache.getInstance().getPhotoForUser(dialog.user);
						}		
					}
					else
					{
						User user = new User();
						user.first_name = "Loading";
						user.last_name = "";
						user.photo = "";
						user.uid = dialog.fwd_messages.get(i).uid;
						dialog.fwd_messages.get(i).user = user;
						dummyUsers.add(user);
					}
					missedUsers.add(dialog.fwd_messages.get(i).uid);
				}
				addUsers(dummyUsers);
			}
		}
		if(networkUpdate && missedUsers.size()>0)
			getUserTask().execute(missedUsers.toArray(new Integer[0]));
	}
	
	private void sortDialogs()
	{
		Collections.sort(dialogList, new Comparator<Message>() {

			@Override
			public int compare(Message lhs, Message rhs) {
				return (int) (rhs.date - lhs.date);
			}
		});
	}
	
	
	private void sortFriends()
	{
		Collections.sort(friends, new Comparator<User>() {

			@Override
			public int compare(User lhs, User rhs) {
				if(lhs.hintpos>=0 && lhs.hintpos<5 && rhs.hintpos>=0 && lhs.hintpos<5)
					return lhs.hintpos-rhs.hintpos;
				if(lhs.hintpos>=0 && lhs.hintpos<5)
					return -1;
				if(rhs.hintpos>=0 && rhs.hintpos<5)
					return 1;
			
				return lhs.first_name.compareTo(rhs.first_name);
			}
		});
	}

	private void sortMessages(int conversationId)
	{
		
		if(userMessagesMap==null || !userMessagesMap.containsKey(conversationId))
			return;
		List<Message> messages = userMessagesMap.get(conversationId);
		Collections.sort(messages, new Comparator<Message>() {
	
				@Override
				public int compare(Message lhs, Message rhs) {
					return (int) (lhs.date - rhs.date);
				}
		});
	}
	

	public boolean addUsers(final List<User> users) {
		boolean added = false;
		synchronized (this) {
			if(users==null)
				return false;
			int size = users.size();
			for (int i =0;i<size;i++) {
				User user = users.get(i);
				if (usersMap.containsKey(user.uid)) {
					
					User contains = usersMap.get(user.uid);
					contains.online = user.online;
					contains.photo = user.photo;
					contains.first_name = user.first_name;
					contains.last_name = user.last_name;
					contains.last_seen = user.last_seen;
					if(user.hintpos>=0)
						contains.hintpos = user.hintpos;
					contains.request = user.request;
					if(user.photo_medium!=null)
						contains.photo_medium = user.photo_medium;
					users.set(i, contains);
				} else {
					synchronized (usersMap) {						
						usersMap.put(user.uid, user);
						added = true;
					}
				}
			}
		}

		new Thread(new Runnable() {

			@Override
			public void run() {
				dbHelper.updateUsers(users, VKApplication.getInstance()
						.getCurrentUser().uid);
			}
		}).start();
		return added;
	}

	public List<User> getUsersById(Integer[] uids,boolean performUpdate) {
		List<User> result = new ArrayList<User>();
		boolean missed = false;
		for(int uid : uids)
		{
			if(usersMap.containsKey(uid))
				result.add(usersMap.get(uid));
			else
			{
				missed = true;
				User loading = new User();
				loading.uid = uid;
				loading.first_name = "Loading...";
				loading.last_name = "";
				result.add(loading);
			}
		}
		if(missed && performUpdate)
		{
			getUserTask().execute(uids);		
		}
		return result;
	}

	public User getUserById(int uid,boolean performUpdate) {
		if (usersMap.containsKey(uid))
		{
			return usersMap.get(uid);
		}
		else if(performUpdate)
		{
			getUserTask().execute(uid);		
		}
		return null;
	}

	public Message getChatById(int chat_id)
	{
		if(dialogMap.containsKey(chat_id))
			return dialogMap.get(chat_id);		
		return null;
	}
	
	public int getDialogsUnreadCount() {
		int result = 0;
		synchronized (dialogList) {			
			for (int i = 0; i < dialogList.size(); i++) {
				if (dialogList.get(i).read_state)
					break;
				result++;
			}
		}
		return result;
	}

	
	
	public List<Message> getIncomingMessages()
	{
		return incomingMessages;
	}
	
	public List<User> getRequestList()
	{
		if(requestsList==null)
			requestsList = new ArrayList<User>();
		return requestsList;
	}
	
	public List<User> getFriends()
	{
		if(friends==null)
		{
			friends = new ArrayList<User>();
			for(User user : usersMap.values())
			{
				if(user.hintpos>=0)
					friends.add(user);
			}
			sortFriends();
 
		}
		return friends;
	}
	
	public List<ContactName> getContacts()
	{
		if(hashPhones==null || hashPhones.size()==0)
			return null;
		if(contactsList==null || contactsList.size()!=hashPhones.size())
		{
			contactsList = new ArrayList<ContactName>();
			for(ContactName user : hashPhones.values()) {
			    if(user.photo!=null || user.photo_bitmap!=null)
			        continue;
                Log.d(VKApplication.TAG, "Contacts read from device");

                InputStream input = openPhotoOfContact(user.contactid);    
                if(input!=null)
                    user.photo_bitmap = BitmapFactory.decodeStream(input);
            }
			contactsList.clear();
			synchronized (contactsList) {			
			    
				for(ContactName user : hashPhones.values())
				{
						contactsList.add(user);
				}
			}
			Collections.sort(contactsList, new Comparator<User>() {


				@Override
				public int compare(User lhs, User rhs) {
					return ((ContactName)lhs).contact_name.compareTo(((ContactName)rhs).contact_name);
				}
			});
			
		}
		return contactsList;	
	}
	
	 public InputStream openPhotoOfContact(long contactId) {
	     Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
	     Uri photoUri = Uri.withAppendedPath(contactUri, Contacts.Photo.CONTENT_DIRECTORY);
	     Cursor cursor = getContentResolver().query(photoUri,
	          new String[] {Contacts.Photo.PHOTO}, null, null, null);
	     if (cursor == null) {
	         return null;
	     }
	     try {
	         if (cursor.moveToFirst()) {
	             byte[] data = cursor.getBlob(0);
	             if (data != null) {
	                 return new ByteArrayInputStream(data);
	             }
	         }
	     } finally {
	         cursor.close();
	     }
	     return null;
	 }
	 
	public List<User> getAllUsers()
	{
		if(usersMap==null)
			return new ArrayList<User>();
		if(allUsers==null || allUsers.size()!=usersMap.size())
		{
			allUsers = new ArrayList<User>();
			synchronized (allUsers) {		
				synchronized (usersMap) {					
					for(User user : usersMap.values())
					{
						allUsers.add(user);
					}
				}
		}
			Collections.sort(allUsers, new Comparator<User>() {
				
				@Override
				public int compare(User lhs, User rhs) {
//					if(lhs.hintpos>=0 && lhs.hintpos<10 && rhs.hintpos>=0 && rhs.hintpos<10)
//						return lhs.hintpos-rhs.hintpos;
//					if(lhs.hintpos>=0 && lhs.hintpos<10)
//						return -1;
//					if(rhs.hintpos>=0 && rhs.hintpos<10)
//						return 1;
					int compare = lhs.first_name.compareTo(rhs.first_name);
					return compare!=0 ? compare : lhs.last_name.compareTo(rhs.last_name);
				}
			});
		}

		return allUsers;
	}
	
	public synchronized User[] getUsers() {
		return usersMap.values().toArray(new User[0]);
	}
	


	public List<Message> getDialogs() {
		return dialogList;
	}
	
	public ContactName getContactById(int id)
	{
		if(hashPhones==null)
			return null;
		for(ContactName contact : hashPhones.values())
		{
			if(contact.contactid==id)
				return contact;
		}
		return null;
	}

	private void setFriends(List<User> friends)
	{
		this.friends = (ArrayList<User>) friends;
		sortFriends();
	}
	 
	public void clearCache()
	{
		dialogList.clear();
		dialogMap.clear();
		messagesMap.clear();
		synchronized (usersMap) {
			usersMap.clear();
		}
		longpollManager.stop();
	}
	
	public void notifyUserIncomingMessage()
	{
		if(watchers.size()==0)
		{
			NotificationUtil.notifyUserIncomingMessages(RecordsProvider.this, incomingMessages, incomingMessages.size());
		}
		else
		{
			for(int i = 0;i<incomingMessages.size();i++)
			{
				Message message = incomingMessages.get(i);
				if(!message.out && !message.read_state)
				{
					SoundUtil.playIncomingMessage(RecordsProvider.this);
					break;
				}
			}
		}
	}
	
	public boolean isForeground() {
		return watchers.size() >0;
	}

	public void clearIncomingMessagesCount()
	{
		incomingMessages.clear();
	}
	
	
	public void syncContacts()
	{
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				Cursor cur = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,null,null,null);
				if(hashPhones==null)
					hashPhones = new HashMap<String, ContactName>();
				if(cur.moveToFirst())
				{
					ArrayList<String> phones = new ArrayList<String>();
					int phone = cur.getColumnIndex(Phone.NUMBER);
					int name = cur.getColumnIndex(Phone.DISPLAY_NAME);
					int id = cur.getColumnIndex(ContactsContract.Contacts._ID);
                    int photoid = cur.getColumnIndex(ContactsContract.Contacts.PHOTO_ID);
					do
					{
					//	Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, cur.getLong(id));
					//    InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(getContentResolver(), uri);			
					    ContactName contact = new ContactName();
					    contact.phones = cur.getString(phone);
					    contact.contact_name = cur.getString(name);
					    contact.photoid = cur.getLong(photoid);
					    contact.contactid = cur.getInt(id);
					    hashPhones.put(contact.phones, contact);
						phones.add(cur.getString(phone));
					} while(cur.moveToNext());
					cur.close();
					Log.d(VKApplication.TAG, "Contacts read from device");
					new GetUsersByPhonesTask(new AsyncCallback<List<User>>() {

						@Override
						public void OnSuccess(List<User> str) {
		                    Log.d(VKApplication.TAG, "Contacts synced with server");
		         //           Cursor cur = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,null,null,null);
							for(int i = 0;i<str.size();i++)
							{
								User user = str.get(i);
								ContactName contact = hashPhones.get(str.get(i).phones);
								if(contact==null)
									continue;
								contact.first_name = user.first_name;
								contact.last_name = user.last_name;
								contact.photo = user.photo;
								contact.uid = user.uid;
							}
					//		cur.close();
							new Thread(new Runnable() {
								
								@Override
								public void run() {
									dbHelper.updateContacts(getContacts(), VKApplication.getInstance().getCurrentUser().uid);
								}
							}).start();;
							notifyWatchers(true);
						}

						@Override
						public void OnError(ErrorCode errorCode) {
						}
					}).execute(phones.toArray(new String[phones.size()]));
				}
			}
		}).start();
	}
	

	
	private void populateDummyData()
	{
		List<Message> dialogs = new ArrayList<Message>();
		Message message = new Message();
		message.body = "С Днем рождения, уважаемый Иоганн!";
		message.uid = 90153847;
		message.out = true;
		message.date = System.currentTimeMillis()/1000;
		message.mid = 10;
		dialogs.add(message);
		message = new Message();
		message.body = "Конкурс приложений для Android в самом разгаре!";
		message.uid = 90153847;
		message.out = false;
		message.chat_id = 3;
		message.date = System.currentTimeMillis()/1000;
		message.mid = 9;
		message.chat_users = new ArrayList<User>();

		User user = new User();
		user.uid = 1;
		message.chat_users.add(user);
		user = new User();
		user.uid = 2;
		message.chat_users.add(user);
		message.title = "Конкурс Android";
		message.chat_active = "1,6492";
		dialogs.add(message);
		message = new Message();
		message.body = "Добрый день, Марат";
		message.uid = 53083705;
		message.out = false;
		message.read_state = true;
		message.date = System.currentTimeMillis()/1000-2;
		message.mid = 6;
		dialogs.add(message);
		message = new Message();
		message.body = "Все сообщения выше - ФЕЙК!";
		message.uid = VKApplication.getInstance().getCurrentUser().uid;
		message.out = true;
		message.date = System.currentTimeMillis()/1000-50;
		message.mid = 4;
		dialogs.add(message);
		message = new Message();
		message.body = "Alea jacta est";
		message.uid = 21282729;
		message.out = false;
		message.date = System.currentTimeMillis()/1000-10;
		message.mid = 3;
		dialogs.add(message);
		
	/*	message = new Message();
		message.body = "Всё, что человек говорит не из собственного опыта, то недостойно доверия. Даже если он цитирует меня";
		message.uid = 42710344;
		message.out = false;
		message.date = System.currentTimeMillis()/1000;
		message.mid = 2;
		dialogs.add(message);
		*/
		addDialogs(dialogs, true);
		populateMessageUsers(dialogs, true);
		notifyWatchers();
	}
	
	
	
	private void setOnline()
	{
		int mode = SettingsUtil.getOnlineShowing(this);
		if(mode==SettingsUtil.ONLINE_NEVER)
			return;
		if(mode==SettingsUtil.ONLINE_LAUNCH && watchers.size()==0)
			return;
		new SetOnlineTask(null).execute();
		new Handler().postDelayed(new Runnable() {
			
			@Override
			public void run() {
				setOnline();
			}
		}, 800000);
	}
	
	public void registerGCM() {
	    try {
        	GCMRegistrar.checkDevice(this);
        	GCMRegistrar.checkManifest(this);
        	final String regId = GCMRegistrar.getRegistrationId(this);
        	if (regId.equals("")) {
        	  GCMRegistrar.register(this, PASTE YOUR GCMPROJECTID HERE);
        	} else {
        	  Log.v(VKApplication.TAG, "GCM Already registered");
        	}
	    } catch (Exception e) {
	        
	    }
	}
	
	public void unregisterGCM() {
		GCMRegistrar.unregister(this);
	}


}
