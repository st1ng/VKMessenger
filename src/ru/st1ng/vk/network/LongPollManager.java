package ru.st1ng.vk.network;

import java.util.ArrayList;

import ru.st1ng.vk.VKApplication;
import ru.st1ng.vk.model.LongPollUpdate;
import ru.st1ng.vk.network.async.LongPollTask;
import ru.st1ng.vk.network.async.LongPollTask.LongPollHandler;
import android.content.Context;

public class LongPollManager {

	
	
	
	LongPollTask longTask;
	private Context context;
	public LongPollManager(Context context)
	{
		watchers = new ArrayList<LongPollManager.LongPollWatcher>();
		this.context = context;
	}

	private ArrayList<LongPollWatcher> watchers;
	public interface LongPollWatcher
	{
		public void OnUpdate(LongPollUpdate update);
	}
	
	public void addWatcher(LongPollWatcher watcher)
	{
		watchers.add(watcher);
	}
	
	
	public void start()
	{
		if(longTask==null || longTask.isCancelled())
		{
			longTask = new LongPollTask(new LongPollHandler() {
				
				@Override
				public void OnUpdate(LongPollUpdate update) {
					for(LongPollWatcher watcher : watchers)
					{
						watcher.OnUpdate(update);
					}
					
				}
				
				@Override
				public void OnError(ErrorCode errorCode) {
					// TODO Auto-generated method stub
					
				}
			}, VKApplication.getInstance().getCurrentUser().token, context);
			longTask.execute();
		}
	}
	
	public void stop()
	{
		if(longTask!=null && !longTask.isCancelled())
			longTask.cancel(true);
	}
	
	public void setForeground()
	{
		if(longTask!=null)
			longTask.setForeground();		
	}
	
	public void setBackground()
	{
		if(longTask!=null)
			longTask.setBackground();
	}
	
	/**
	 * SingletonHolder is loaded on the first execution of
	 * DataProvider.getInstance() or the first access to
	 * SingletonHolder.INSTANCE, not before.
	 */
	private static class SingletonHolder {
		public static LongPollManager INSTANCE;
	}

	public static LongPollManager getInstance(Context context) {
		if(SingletonHolder.INSTANCE==null)
			SingletonHolder.INSTANCE = new LongPollManager(context);
		return SingletonHolder.INSTANCE;
	}
	

	
}
