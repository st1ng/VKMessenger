package ru.st1ng.vk.activity;

import com.google.android.gcm.GCMRegistrar;

import ru.st1ng.vk.R;
import ru.st1ng.vk.VKApplication;
import ru.st1ng.vk.data.RecordsProvider;
import ru.st1ng.vk.data.RecordsProvider.LocalBinder;
import ru.st1ng.vk.network.LongPollManager;
import ru.st1ng.vk.util.FontsUtil;
import ru.st1ng.vk.util.HttpUtil;
import ru.st1ng.vk.util.NotificationUtil;
import ru.st1ng.vk.util.SettingsUtil;
import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

public class TabhostActivity extends TabActivity{

	
	private TabHost tabHost;
	private TextView notifyMessagesCount;
	private TextView notifyRequestsCount;
	boolean mBound = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_tabhost);
		LongPollManager.getInstance(this).start();
		
		tabHost = getTabHost();
		initTabs();
		checkVersion();
	}
	
	


	private void initTabs()
	{
		TabSpec spec;
		Intent intent;
		
		tabHost.clearAllTabs();
		
		intent = new Intent(this, MessagesActivity.class);
		intent.setData(getIntent().getData());
		spec = tabHost.newTabSpec("messages").setContent(intent).setIndicator(getTabView(R.string.messages, R.drawable.ic_msg_selector));
		tabHost.addTab(spec);
		
		intent = new Intent(this, ContactsActivity.class);
		intent.setData(getIntent().getData());
		spec = tabHost.newTabSpec("contacts").setContent(intent).setIndicator(getTabView(R.string.contacts, R.drawable.ic_contacts_selector));
		tabHost.addTab(spec);

		intent = new Intent(this, SearchActivity.class);
		intent.setData(getIntent().getData());
		spec = tabHost.newTabSpec("search").setContent(intent).setIndicator(getTabView(R.string.search, R.drawable.ic_search_selector));
		tabHost.addTab(spec);

		intent = new Intent(this, SettingsActivity.class);
		intent.setData(getIntent().getData());
		spec = tabHost.newTabSpec("settings").setContent(intent).setIndicator(getTabView(R.string.settings, R.drawable.ic_settings_selector));
		tabHost.addTab(spec);
}
	
    private View getTabView(int nameResourceId, int drawableResourceId)
    {
    	View result = getLayoutInflater().inflate(R.layout.tab_widget, null);
    	TextView text = (TextView) result.findViewById(R.id.tabsText);
    	text.setText(nameResourceId);
    	text.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
    	ImageView image = (ImageView) result.findViewById(R.id.tabsImage);
    	image.setImageResource(drawableResourceId);
    	TextView notifyCount = (TextView) result.findViewById(R.id.notifyCountText);
    	notifyCount.setVisibility(View.INVISIBLE);
    	notifyCount.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
    	if(nameResourceId==R.string.messages)
    		notifyMessagesCount = notifyCount;
    	else if(nameResourceId==R.string.search)
    		notifyRequestsCount = notifyCount;
    	return result;
    }

    public void setNotifyMessagesCount(int count)
    {
    	if(count==0)
    	{
    		notifyMessagesCount.setVisibility(View.INVISIBLE);
    		return;
    	}
    	notifyMessagesCount.setVisibility(View.VISIBLE);
    	notifyMessagesCount.setText(count + "");
    }

    public void setRequestsCount(int count)
    {
    	if(count==0)
    	{
    		notifyRequestsCount.setVisibility(View.INVISIBLE);
    		return;
    	}
    	notifyRequestsCount.setVisibility(View.VISIBLE);
    	notifyRequestsCount.setText(count + "");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	VKApplication.getInstance().logOff();
    	stopService(new Intent(TabhostActivity.this, RecordsProvider.class));
    	startActivity(new Intent(TabhostActivity.this, LoginActivity.class));    	
    	this.finish();    	
    	return super.onOptionsItemSelected(item);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	tabHost.setCurrentTab(3);
    	return super.onPrepareOptionsMenu(menu);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    //	menu.add("Log off");
    	return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    }
    
    @Override
    protected void onResume() {
    	int user = getIntent().getIntExtra(ConversationActivity.EXTRA_USERID, 0);
    	if(user!=0)
    	{
    		getIntent().putExtra(ConversationActivity.EXTRA_USERID, 0);
    		Intent conversation = new Intent(TabhostActivity.this, ConversationActivity.class);
    		conversation.putExtra(ConversationActivity.EXTRA_USERID, user);
    		startActivity(conversation);
    	}
    	NotificationUtil.clearNotifications();
    	super.onResume();
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
    	Bundle extras = intent.getExtras();
    	if(extras!=null)
    	{
    		
        	int user = extras.getInt(ConversationActivity.EXTRA_USERID, 0);
        	if(user!=0)
        	{
        		Intent conversation = new Intent(TabhostActivity.this, ConversationActivity.class);
        		Bundle extrasNew = new Bundle();
        		extrasNew.putInt(ConversationActivity.EXTRA_USERID, user);
        		conversation.putExtras(extrasNew);
        		conversation.putExtra(ConversationActivity.EXTRA_USERID, user);
        		conversation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        		conversation.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        		conversation.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        		startActivity(conversation);
        	}    		
    	}
    	super.onNewIntent(intent);
    }
    
    //Hack to place profile photo. We have settings inside tabHost, so onActivityResult cannot be handled there
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    }
    private void checkVersion()
    {
    		new Thread(new Runnable() {
				
				@Override
				public void run() {
				    
			    	try
			    	{
			    		String ver = HttpUtil.getHttp("http://st1ng.ru/vkversion.txt", null);
			    		String changelog = HttpUtil.getHttp("http://st1ng.ru/vkchangelog.txt", null);
			    		Integer version = Integer.parseInt(ver.split("\n")[0]);
			    		if(SettingsUtil.needToUpdate(TabhostActivity.this, version, getPackageManager().getPackageInfo(getPackageName(), 0)))
			    		{
			    			final AlertDialog.Builder dialog = new AlertDialog.Builder(TabhostActivity.this);
			    			dialog.setTitle(R.string.new_version_available).setMessage(getString(R.string.changelog) + "\n" + changelog).setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									Intent intent = new Intent(Intent.ACTION_VIEW);
									intent.setData(Uri.parse("market://details?id=ru.st1ng.vk"));
									startActivity(intent);
								}
							}).setNegativeButton(R.string.cancel, null);
			    			runOnUiThread(new Runnable() {
								
								@Override
								public void run() {
									dialog.show();
								}
							});
			    			return;
			    		}
			    		
			    		if(SettingsUtil.runCount(TabhostActivity.this) && !SettingsUtil.isRated(TabhostActivity.this))
			    		{
			    			final AlertDialog.Builder dialog = new AlertDialog.Builder(TabhostActivity.this);
			    			dialog.setTitle(R.string.i_need_your_help).setMessage(R.string.please_rate).setPositiveButton(R.string.rate, new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									Intent intent = new Intent(Intent.ACTION_VIEW);
									intent.setData(Uri.parse("market://details?id=ru.st1ng.vk"));
									startActivity(intent);
									SettingsUtil.setRated(TabhostActivity.this, true);
								}

				    		}).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									SettingsUtil.setRated(TabhostActivity.this, false);									
								}
								
							});
			    			runOnUiThread(new Runnable() {
	 							
								@Override
								public void run() {
									dialog.show();
								}
							});
			    		}
		        	} catch (Exception e)
		        	{
		        		
		        	}
				}
			}).start();
    }
    
    
}
