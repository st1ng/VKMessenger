package ru.st1ng.vk.activity;

import ru.st1ng.vk.R;
import ru.st1ng.vk.VKApplication;
import ru.st1ng.vk.data.RecordsProvider;
import ru.st1ng.vk.data.RecordsProvider.LocalBinder;
import ru.st1ng.vk.data.RecordsProvider.RecordsWatcher;
import ru.st1ng.vk.network.ErrorCode;
import ru.st1ng.vk.util.FontsUtil;
import ru.st1ng.vk.views.adapter.AllUsersListAdapter;
import ru.st1ng.vk.views.adapter.ContactListAdapter;
import ru.st1ng.vk.views.adapter.FriendListAdapter;
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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class ComposeMessageActivity extends Activity {

	ListView friendsListView;
	AllUsersListAdapter friendAdapter;
	View headerView;
	EditText searchView;
	ImageView clearSearchView;
	TextView titleText;
	RecordsProvider recordsManager;
	RecordsWatcher recordsWatcher;
	boolean mBound = false;

	public static final String EXTRA_CHOOSEMODE = "choosemode";
	public static final String EXTRA_RESULT_USERID = "result";
	private boolean chooseMode = false;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_compose);
		chooseMode = getIntent().getBooleanExtra(EXTRA_CHOOSEMODE, false);
		friendsListView = (ListView) findViewById(R.id.usersList);
		titleText = (TextView) findViewById(R.id.titleText);
		titleText.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);

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

		findViewById(R.id.frameBack).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});
	}
	
	
	private void serviceConnected()
	{
		friendAdapter = new AllUsersListAdapter(this, recordsManager.getAllUsers(),getIntent().getStringExtra(ConversationActivity.EXTRA_FORWARD_MESSAGES), chooseMode);
		friendsListView.setAdapter(friendAdapter);
		
		recordsWatcher = new RecordsWatcher() {
			
			@Override
			public void OnChangedRecords(final boolean needInvalidate) {
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						if(needInvalidate)
						{
							synchronized (recordsManager) {
									friendAdapter.setItems(recordsManager.getAllUsers());
								friendAdapter.getFilter().filter(searchView.getText());
						//		friendAdapter.notifyDataSetChanged();
							}
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
}
