package ru.st1ng.vk.activity;

import java.lang.ref.SoftReference;
import java.lang.reflect.Type;

import ru.st1ng.vk.R;
import ru.st1ng.vk.VKApplication;
import ru.st1ng.vk.data.RecordsProvider;
import ru.st1ng.vk.data.RecordsProvider.LocalBinder;
import ru.st1ng.vk.data.RecordsProvider.RecordsWatcher;
import ru.st1ng.vk.model.Message;
import ru.st1ng.vk.network.ErrorCode;
import ru.st1ng.vk.network.async.FriendsGetTask;
import ru.st1ng.vk.util.FontsUtil;
import ru.st1ng.vk.util.UIUtil;
import ru.st1ng.vk.views.adapter.ContactListAdapter;
import ru.st1ng.vk.views.adapter.DialogListAdapter;
import ru.st1ng.vk.views.adapter.DialogWrapper;
import ru.st1ng.vk.views.adapter.FriendListAdapter;
import ru.st1ng.vk.views.adapter.FriendWrapper;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ToggleButton;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;

public class ContactsActivity extends Activity {

	ListView friendsListView;
	FriendListAdapter friendAdapter;
	ListView contactsListView;
	ContactListAdapter contactsAdapter;
	
	ImageView updatingView;
	AnimationDrawable updatingDrawable;
	ToggleButton toggleFriends;
	ToggleButton toggleOnline;
	ToggleButton toggleContacts;
	
	View headerView;
	EditText searchView;
	ImageView clearSearchView;
	ImageView contactsLoading;
	
	RecordsProvider recordsManager;
	RecordsWatcher recordsWatcher;
	boolean mBound = false;
	boolean contactsMode = false;
	boolean contactsSynced = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_contacts);
		
		updatingView = (ImageView) findViewById(R.id.updatingImage);
		updatingDrawable = (AnimationDrawable) updatingView.getDrawable();
		friendsListView = (ListView) findViewById(R.id.dialogList);
		contactsListView = (ListView) findViewById(R.id.contactsList);
		toggleFriends = (ToggleButton) findViewById(R.id.toggleFriends);
		toggleOnline = (ToggleButton) findViewById(R.id.toggleOnline);
		toggleContacts = (ToggleButton) findViewById(R.id.toggleContacts);
		toggleFriends.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
		toggleOnline.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
		toggleContacts.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);

		headerView = getLayoutInflater().inflate(R.layout.widget_search, null);
		friendsListView.addHeaderView(headerView);
		searchView = (EditText) headerView.findViewById(R.id.search);
		searchView.setTypeface(FontsUtil.Helvetica);
		clearSearchView = (ImageView) headerView.findViewById(R.id.clear_field);
		clearSearchView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				searchView.setText("");
			}
		});
		searchView.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				clearSearchView.setVisibility(s.length()>0 ? View.VISIBLE : View.INVISIBLE);

				if(friendAdapter!=null)
					if(s.length()>0)
						friendAdapter.getFilter().filter(s);
					else
						friendAdapter.getFilter().filter(null);
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				
			}
		});

		toggleFriends.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(friendAdapter!=null)
				{
					friendAdapter.setMode(false);
					friendAdapter.getFilter().filter(searchView.getText());
				}
				toggleOnline.setChecked(false);
				toggleContacts.setChecked(false);
				findViewById(R.id.contactsFrame).setVisibility(View.INVISIBLE);
				findViewById(R.id.dialogList).setVisibility(View.VISIBLE);

			}
		});
		toggleOnline.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(friendAdapter!=null)
				{
					friendAdapter.setMode(true);
					friendAdapter.getFilter().filter(searchView.getText());
				}
				toggleFriends.setChecked(false);
				toggleContacts.setChecked(false);
				findViewById(R.id.contactsFrame).setVisibility(View.INVISIBLE);
				findViewById(R.id.dialogList).setVisibility(View.VISIBLE);

			}
		});
		toggleContacts.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				toggleFriends.setChecked(false);
				toggleOnline.setChecked(false);
				findViewById(R.id.contactsFrame).setVisibility(View.VISIBLE);
				findViewById(R.id.dialogList).setVisibility(View.INVISIBLE);
				if(!contactsSynced)
				{
					findViewById(R.id.synchronizeFrame).setVisibility(View.VISIBLE);
					findViewById(R.id.contactsList).setVisibility(View.INVISIBLE);					
				}
				else
				{
					findViewById(R.id.synchronizeFrame).setVisibility(View.INVISIBLE);
					findViewById(R.id.contactsList).setVisibility(View.VISIBLE);					
				}
			}
		});
		
		OnCheckedChangeListener checkedChange = new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					buttonView.setEnabled(!isChecked);
			}
		};
		
		toggleFriends.setOnCheckedChangeListener(checkedChange);
		toggleOnline.setOnCheckedChangeListener(checkedChange);
		toggleContacts.setOnCheckedChangeListener(checkedChange);
		
		
		contactsLoading = (ImageView) findViewById(R.id.contactsLoadImage);
		findViewById(R.id.buttonSync).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(mBound)
				{
					findViewById(R.id.buttonSync).setVisibility(View.GONE);
					contactsLoading.setVisibility(View.VISIBLE);
					((AnimationDrawable)contactsLoading.getDrawable()).start();
					recordsManager.syncContacts();
				}
			}
		});
	}
	
	
	private void serviceConnected()
	{
		toggleContacts.setVisibility(VKApplication.getInstance().getCurrentUser().has_mobile ? View.VISIBLE : View.GONE);
		contactsSynced = recordsManager.getContacts()!=null;
			
//		if(recordsManager.get)
//		recordsManager.syncContacts();
		friendAdapter = new FriendListAdapter(this, recordsManager.getFriends());
		friendsListView.setAdapter(friendAdapter);
		contactsAdapter = new ContactListAdapter(this, recordsManager.getContacts());
		contactsListView.setAdapter(contactsAdapter);
		
		recordsWatcher = new RecordsWatcher() {
			
			@Override
			public void OnChangedRecords(final boolean needInvalidate) {
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						updatingView.setVisibility(View.INVISIBLE);
						updatingDrawable.stop();
						if(needInvalidate)
						{
							synchronized (recordsManager) {
								if(findViewById(R.id.contactsFrame).getVisibility()==View.VISIBLE)
								{
									contactsSynced = recordsManager.getContacts()!=null;
									if(contactsSynced)
									{
										contactsAdapter.setItems(recordsManager.getContacts());
										findViewById(R.id.synchronizeFrame).setVisibility(View.INVISIBLE);
										findViewById(R.id.contactsList).setVisibility(View.VISIBLE);
										contactsListView.setVisibility(View.VISIBLE);									
										contactsAdapter.notifyDataSetChanged();
										}
									else
									{
										findViewById(R.id.synchronizeFrame).setVisibility(View.VISIBLE);
										findViewById(R.id.contactsList).setVisibility(View.INVISIBLE);					
									}
								}
								else
								{
									friendAdapter.setItems(recordsManager.getFriends());
								}
								friendAdapter.getFilter().filter(searchView.getText());
						//		friendAdapter.notifyDataSetChanged();
							}
						}
						if(recordsManager.getLastError()==ErrorCode.NoError)
						{
//							errorImage.setVisibility(View.INVISIBLE);
//							openNewDialog.setVisibility(View.VISIBLE);
						}
						
					}
				});
			}
			
			@Override
			public void OnError(ErrorCode errorCode) {
			/*	if(errorCode==ErrorCode.NetworkUnavailable)
				{
					if(errorImage.getVisibility()==View.INVISIBLE)
						UIUtil.showToast(MessagesActivity.this, getString(R.string.network_unavailable_show_cached));
					Log.d(VKApplication.TAG, "Network unavailable. Show only cached dialogs");
					errorImage.setVisibility(View.VISIBLE);
					openNewDialog.setVisibility(View.INVISIBLE);	
					errorImage.setOnClickListener(new OnClickListener() {
						
						@Override
						public void onClick(View v) {
							UIUtil.showToast(MessagesActivity.this, getString(R.string.network_unavailable_show_cached));
						}
					});
				}*/
			}
		};
		
		recordsManager.addWatcher(recordsWatcher);

		recordsManager.performFriendsUpdate();
//		recordsManager.performDialogsUpdate(20, 0,this);
		updatingView.setVisibility(View.VISIBLE);
		updatingDrawable.start();
	}
	
    @Override
    public boolean onSearchRequested() {
        searchView.requestFocus();
        return false;
    }

	@Override
	protected void onStart() {
		super.onStart();
		getApplicationContext().bindService(new Intent(this, RecordsProvider.class), mConnection, Context.BIND_AUTO_CREATE);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}
	@Override
	protected void onStop() {
		super.onStop();
        if (mBound) {
            recordsManager.removeWatcher(recordsWatcher);
            getApplicationContext().unbindService(mConnection);
            mBound = false;
        }
	};

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            recordsManager = binder.getService();
            mBound = true;
            serviceConnected();
    		recordsManager.addWatcher(recordsWatcher);
    		
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
    
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        recordsManager.syncContacts();
        return true;
    };
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        menu.add("Sync contacts");
        return true;
    };
}
