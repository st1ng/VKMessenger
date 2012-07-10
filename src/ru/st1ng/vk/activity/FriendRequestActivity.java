package ru.st1ng.vk.activity;

import ru.st1ng.vk.R;
import ru.st1ng.vk.data.RecordsProvider;
import ru.st1ng.vk.data.RecordsProvider.LocalBinder;
import ru.st1ng.vk.data.RecordsProvider.RecordsWatcher;
import ru.st1ng.vk.model.User;
import ru.st1ng.vk.network.ErrorCode;
import ru.st1ng.vk.network.async.BasicAsyncTask.AsyncCallback;
import ru.st1ng.vk.network.async.FriendRequestCancelTask;
import ru.st1ng.vk.network.async.FriendRequestSubmitTask;
import ru.st1ng.vk.views.WebImageView;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class FriendRequestActivity extends Activity {

	
	TextView titleText;
	Button requestSubmit;
	TextView requestCancel;
	WebImageView profileImage;
	
	public static final String EXTRA_USERID = "userid";
	public static final String EXTRA_REQUEST_IN = "inrequest";
	
	RecordsProvider recordsManager;
	
	private int userid;
	private boolean inrequest;
	
	private boolean mBound;
	
	RecordsWatcher recordsWatcher;
	
	ProgressDialog progress;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_friendrequest);
		titleText = (TextView) findViewById(R.id.titleText);
		requestSubmit = (Button) findViewById(R.id.requestSubmit);
		requestCancel = (TextView) findViewById(R.id.requestCancel);
		profileImage = (WebImageView) findViewById(R.id.profileImage);
		userid = getIntent().getIntExtra(EXTRA_USERID, 0);
		inrequest = getIntent().getBooleanExtra(EXTRA_REQUEST_IN, false);
		if(!inrequest)
			requestCancel.setVisibility(View.INVISIBLE);
		progress = new ProgressDialog(this);
		progress.setMessage(getString(R.string.please_wait));
		
		findViewById(R.id.frameBack).setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
		
		recordsWatcher = new RecordsWatcher() {
			
			@Override
			public void OnError(ErrorCode errorCode) {
				
			}
			
			@Override
			public void OnChangedRecords(boolean needInvalidate) {
				User contact = recordsManager.getUserById(userid,false);
				if(contact!=null)
				{
					if(contact.photo_medium!=null)
						profileImage.setImageFromURL(contact.photo_medium);
				}				
			}
		};
		
		requestSubmit.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				progress.show();
				new FriendRequestSubmitTask(new AsyncCallback<Boolean>() {

					@Override
					public void OnSuccess(Boolean str) {
						progress.cancel();
						AlertDialog.Builder dialog = new AlertDialog.Builder(FriendRequestActivity.this);
						dialog.setMessage(inrequest ? R.string.successfuly_submited : R.string.successfuly_send_request).setPositiveButton("OK", new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								FriendRequestActivity.this.finish();								
							}
						}).show();
						recordsManager.performRequestsUpdate();
					}

					@Override
					public void OnError(ErrorCode errorCode) {
						progress.cancel();
						AlertDialog.Builder dialog = new AlertDialog.Builder(FriendRequestActivity.this);
						dialog.setMessage(errorCode.getStringResource()).setPositiveButton("OK", null).show();
						
					}
				}).execute(userid);

			}
		});
		
		requestCancel.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				progress.show();
				new FriendRequestCancelTask(new AsyncCallback<Boolean>() {

					@Override
					public void OnSuccess(Boolean str) {
						progress.cancel();
						AlertDialog.Builder dialog = new AlertDialog.Builder(FriendRequestActivity.this);
						dialog.setMessage(R.string.successfuly_canceled).setPositiveButton("OK", new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								FriendRequestActivity.this.finish();								
							}
						}).show();
						recordsManager.performRequestsUpdate();
					}

					@Override
					public void OnError(ErrorCode errorCode) {
						progress.cancel();
						AlertDialog.Builder dialog = new AlertDialog.Builder(FriendRequestActivity.this);
						dialog.setMessage(errorCode.getStringResource()).setPositiveButton("OK", null).show();	
					}
				}).execute(userid);
			}
		});
	}
	
	private void serviceConnected()
	{
		final User contact = recordsManager.getUserById(userid,true);
		if(contact!=null)
		{
			if(contact.photo_medium!=null)
				profileImage.setImageFromURL(contact.photo_medium);
	        titleText.setText(contact.first_name + " " + contact.last_name);
		}
//
//		sendMessage.setVisibility(contact.uid>0 ? View.VISIBLE : View.GONE);
//		sendMessage.setOnClickListener(new OnClickListener() {
//			
//			@Override
//			public void onClick(View v) {
//				Intent intent = new Intent(ContactInfoActivity.this, ConversationActivity.class);
//				intent.putExtra(ConversationActivity.EXTRA_USERID, contact.uid);
//				startActivity(intent);
//			}
//		});
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
			recordsManager.removeWatcher(recordsWatcher);
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
			recordsManager.addWatcher(recordsWatcher);
            serviceConnected();

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
}
