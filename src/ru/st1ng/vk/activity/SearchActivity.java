package ru.st1ng.vk.activity;

import java.util.ArrayList;
import java.util.List;

import ru.st1ng.vk.R;
import ru.st1ng.vk.VKApplication;
import ru.st1ng.vk.data.RecordsProvider;
import ru.st1ng.vk.data.RecordsProvider.LocalBinder;
import ru.st1ng.vk.data.RecordsProvider.RecordsWatcher;
import ru.st1ng.vk.model.User;
import ru.st1ng.vk.network.ErrorCode;
import ru.st1ng.vk.network.async.BasicAsyncTask.AsyncCallback;
import ru.st1ng.vk.network.async.GetSuggestionsTask;
import ru.st1ng.vk.network.async.ImportContactsTask;
import ru.st1ng.vk.network.async.SearchUsersTask;
import ru.st1ng.vk.util.FontsUtil;
import ru.st1ng.vk.util.SettingsUtil;
import ru.st1ng.vk.views.adapter.SearchFriendListAdapter;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class SearchActivity extends Activity {

	ListView friendsListView;
	SearchFriendListAdapter friendAdapter;
	ImageView updatingView;
	AnimationDrawable updatingDrawable;
	View headerView;
	EditText searchView;
	ImageView clearSearchView;
	TextView title;
	EditText search;
	
	RecordsProvider recordsManager;
	RecordsWatcher recordsWatcher;
	boolean mBound = false;
	
	List<User> suggestions;
	List<User> allUsersList;
//	List<User> requests;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_search);
		
		updatingView = (ImageView) findViewById(R.id.updatingImage);
		updatingDrawable = (AnimationDrawable) updatingView.getDrawable();
		friendsListView = (ListView) findViewById(R.id.dialogList);
		title = (TextView) findViewById(R.id.titleText);
		title.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
		headerView = getLayoutInflater().inflate(R.layout.widget_search, null);
		//friendsListView.addHeaderView(headerView);
		searchView = (EditText) headerView.findViewById(R.id.search);
		clearSearchView = (ImageView) headerView.findViewById(R.id.clear_field);
		search = (EditText) findViewById(R.id.search);
		search.addTextChangedListener(new TextWatcher() {
			
			long lastUpdated = 0;
			@Override
			public void onTextChanged(final CharSequence s, int start, int before, int count) {
				if(mBound)
				{
					if(s.length()==0)
					{
						updateSuggestions();
					}
					else if(s.length()>=3)
					{
						updatingView.setVisibility(View.VISIBLE);
						updatingDrawable.start();
						lastUpdated = System.currentTimeMillis();
						new Handler().postDelayed(new Runnable() {
							
							@Override
							public void run() {
								if(System.currentTimeMillis()-lastUpdated<1500)
									return;
								Log.d(VKApplication.TAG, "Search users!");
								searchUsers(s.toString());
							}
						}, 1500);
					}
				}				
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
			
			@Override
			public void afterTextChanged(Editable s) {
			}
		});
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

	}
	
	
	private void serviceConnected()
	{
		if(SettingsUtil.isContactsImported(this))
		{
			Cursor cur = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,null,null,null);
			if(cur.moveToFirst())
			{
				ArrayList<String> phones = new ArrayList<String>();
				int index = cur.getColumnIndex(Phone.NUMBER);
				do
				{
					phones.add(cur.getString(index));
				} while(cur.moveToNext());
				new ImportContactsTask(new AsyncCallback<String>() {

					@Override
					public void OnSuccess(String str) {
						
					}

					@Override
					public void OnError(ErrorCode errorCode) {
						
					}
				}).execute(phones.toArray(new String[phones.size()]));
			}
			//return;
		}

		//requests = recordsManager.getRequestList();
		allUsersList = new ArrayList<User>();
		if(recordsManager.getRequestList()!=null)
			allUsersList.addAll(recordsManager.getRequestList());
		friendAdapter = new SearchFriendListAdapter(this, allUsersList);
		friendsListView.setAdapter(friendAdapter);
		updateSuggestions();
		recordsManager.performRequestsUpdate();
	}
	
	private void updateSuggestions()
	{
//		if(suggestions!=null)
//		{
//			allUsersList.clear();
//			if(recordsManager.getRequestList()!=null)
//				allUsersList.addAll(recordsManager.getRequestList());
//			allUsersList.addAll(suggestions);
//			updatingView.setVisibility(View.INVISIBLE);
//			friendAdapter.setItems(suggestions);
//			friendAdapter.notifyDataSetChanged();			
//		}
//		else
			new GetSuggestionsTask(new AsyncCallback<List<User>>() {
	
				@Override
				public void OnSuccess(List<User> str) {
					if(mBound)
					{
						updatingView.setVisibility(View.INVISIBLE);
						if(search.getText().toString().length()>0)
							return;
						updatingView.setVisibility(View.INVISIBLE);
						suggestions = str;
						allUsersList.clear();
						if(recordsManager.getRequestList()!=null)
							allUsersList.addAll(recordsManager.getRequestList());
						allUsersList.addAll(suggestions);
						friendAdapter.setItems(allUsersList);
		                friendAdapter.setSearchMode(false);
						friendAdapter.notifyDataSetChanged();
						new Handler().postDelayed(new Runnable() {
							
							@Override
							public void run() {
								friendAdapter.notifyDataSetChanged();							
							}
						}, 300);
					}
				}
	
				@Override
				public void OnError(ErrorCode errorCode) {
					updatingView.setVisibility(View.INVISIBLE);
				}
			}, VKApplication.getInstance().getAPILang()).execute();
		
		updatingView.setVisibility(View.VISIBLE);
		updatingDrawable.start();
	}
	
	AsyncTask<String, Void, List<User>> userTask;
	private void searchUsers(String search)
	{
		if(userTask!=null && !userTask.isCancelled())
			userTask.cancel(true);
		userTask = new SearchUsersTask(new AsyncCallback<List<User>>() {

			@Override
			public void OnSuccess(List<User> str) {
				updatingView.setVisibility(View.INVISIBLE);
				allUsersList.clear();
//				if(recordsManager.getRequestList()!=null)
//					allUsersList.addAll(recordsManager.getRequestList());
				allUsersList.addAll(str);
				friendAdapter.setItems(allUsersList);
				friendAdapter.setSearchMode(true);
					friendAdapter.notifyDataSetChanged();
			}

			@Override
			public void OnError(ErrorCode errorCode) {
				updatingView.setVisibility(View.INVISIBLE);
			}
		}, VKApplication.getInstance().getAPILang()).execute(search);
		

		updatingView.setVisibility(View.VISIBLE);
		updatingDrawable.start();
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
            getApplicationContext().unbindService(mConnection);
            mBound = false;
        }
	};

    @Override
    public boolean onSearchRequested() {
        searchView.requestFocus();
        return false;
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            recordsManager = binder.getService();
            mBound = true;
    		recordsManager.addWatcher(recordsWatcher);
            serviceConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
}
