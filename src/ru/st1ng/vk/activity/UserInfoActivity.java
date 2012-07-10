package ru.st1ng.vk.activity;

import ru.st1ng.vk.R;
import ru.st1ng.vk.data.RecordsProvider;
import ru.st1ng.vk.data.RecordsProvider.LocalBinder;
import ru.st1ng.vk.data.RecordsProvider.RecordsWatcher;
import ru.st1ng.vk.model.ContactName;
import ru.st1ng.vk.model.User;
import ru.st1ng.vk.network.ErrorCode;
import ru.st1ng.vk.network.async.FriendRequestCancelTask;
import ru.st1ng.vk.network.async.FriendRequestSubmitTask;
import ru.st1ng.vk.network.async.BasicAsyncTask.AsyncCallback;
import ru.st1ng.vk.util.FontsUtil;
import ru.st1ng.vk.util.UIUtil;
import ru.st1ng.vk.views.WebImageView;
import ru.st1ng.vk.views.adapter.AllUsersListAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class UserInfoActivity extends Activity {
	
	private boolean mBound = false;
	RecordsProvider recordsManager;
	
	int uid;
	
	TextView titleText;
	Button sendMessage;
	Button addFriend;
    Button deleteDialog;
	WebImageView contactImage;
	RecordsWatcher recordsWatcher;
	TextView removeFromFriends;
	ImageView onlineImage;
	
	public static final String EXTRA_USERID = "userid";
	ProgressDialog progress;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_userinfo);
		uid = getIntent().getIntExtra(EXTRA_USERID, -1);
		titleText = (TextView) findViewById(R.id.titleText);
		sendMessage = (Button) findViewById(R.id.buttonSendMessage);
		addFriend = (Button) findViewById(R.id.buttonAddFriend);
		deleteDialog = (Button) findViewById(R.id.buttonDeleteDialog);
		contactImage = (WebImageView) findViewById(R.id.contactImage);
		removeFromFriends = (TextView) findViewById(R.id.removeFromFriends);
		removeFromFriends.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
		onlineImage = (ImageView) findViewById(R.id.onlineImage);
		titleText.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
		addFriend.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
		deleteDialog.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
        sendMessage.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
		
	      sendMessage.setOnClickListener(new OnClickListener() {
	            
	            @Override
	            public void onClick(View v) {
	                if(uid<0)
	                    return;
                    Intent intent = new Intent(UserInfoActivity.this, ConversationActivity.class);
                    intent.putExtra(ConversationActivity.EXTRA_USERID, uid);
                    startActivity(intent);
	            }
	        });
	   
	      deleteDialog.setOnClickListener(new OnClickListener() {
	            
	            @Override
	            public void onClick(View v) {
	                AlertDialog.Builder dialog = new AlertDialog.Builder(UserInfoActivity.this);
	                dialog.setTitle(R.string.please_confirm).setMessage(R.string.do_you_really_want_to_delete_dialog)
	                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

	                    @Override
	                    public void onClick(DialogInterface dialog, int which) {
	                        if(mBound) {
	                            recordsManager.deleteDialogs(uid);
	                            Intent result = new Intent();
	                            result.putExtra(GroupInfoActivity.EXTRA_LEAVE, true);
	                            setResult(RESULT_OK, result);
	                            UserInfoActivity.this.finish();
	                        }
	                    }
	                }).setNegativeButton(android.R.string.no, null).show();
	            }
	        });
		removeFromFriends.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(UserInfoActivity.this);
                dialog.setTitle(R.string.please_confirm).setMessage(R.string.do_you_really_want_to_remove_friend)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        progress.show();
                        new FriendRequestCancelTask(new AsyncCallback<Boolean>() {

                            @Override
                            public void OnSuccess(Boolean str) {
                                progress.cancel();
                                if(mBound) {
                                    AlertDialog.Builder dialog = new AlertDialog.Builder(UserInfoActivity.this);
                                    dialog.setMessage(R.string.successfuly_deleted_user).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent result = new Intent();
                                            result.putExtra(GroupInfoActivity.EXTRA_LEAVE, true);
                                            setResult(RESULT_OK, result);
                                            UserInfoActivity.this.finish();                                
                                        }
                                    }).show();
                                    recordsManager.deleteUser(uid);
                                }
                            }

                            @Override
                            public void OnError(ErrorCode errorCode) {
                                progress.cancel();
                                AlertDialog.Builder dialog = new AlertDialog.Builder(UserInfoActivity.this);
                                dialog.setMessage(errorCode.getStringResource()).setPositiveButton("OK", null).show();
                            }
                        }).execute(uid);
                    }
                }).setNegativeButton(android.R.string.no, null).show();
            }
        });
	      progress = new ProgressDialog(this);
	        progress.setMessage(getString(R.string.please_wait));

		addFriend.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                progress.show();
                new FriendRequestSubmitTask(new AsyncCallback<Boolean>() {

                    @Override
                    public void OnSuccess(Boolean str) {
                        progress.cancel();
                        AlertDialog.Builder dialog = new AlertDialog.Builder(UserInfoActivity.this);
                        dialog.setMessage(R.string.successfuly_send_request).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                UserInfoActivity.this.finish();                                
                            }
                        }).show();
                        recordsManager.performRequestsUpdate();
                    }

                    @Override
                    public void OnError(ErrorCode errorCode) {
                        progress.cancel();
                        AlertDialog.Builder dialog = new AlertDialog.Builder(UserInfoActivity.this);
                        dialog.setMessage(errorCode.getStringResource()).setPositiveButton("OK", null).show();
                        
                    }
                }).execute(uid);                
            }
        });
		findViewById(R.id.frameBack).setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
		
	      recordsWatcher = new RecordsWatcher() {
	            
	            @Override
	            public void OnError(ErrorCode errorCode) {
	                if(errorCode.equals(ErrorCode.MessagesSuccessfullyDeleted))
	                {
	                    Intent result = new Intent();
	                    result.putExtra(GroupInfoActivity.EXTRA_LEAVE, true);
	                    setResult(RESULT_OK, result);
	                    UserInfoActivity.this.finish();
	                }
	            }
	            
	            @Override
	            public void OnChangedRecords(boolean needInvalidate) {
	                runOnUiThread(new Runnable() {
	                    
	                    @Override
	                    public void run() {
	                        updateInfo(false);
	                    }
	                });
	            }
	        };
	}
	
	private void updateInfo(boolean init) {
        final User contact = recordsManager.getUserById(uid, true);
        if(contact==null)
        {
            UIUtil.showToast(this, "User not found");
            this.finish();
            return;
        }
        onlineImage.setVisibility(contact.online ? View.VISIBLE : View.INVISIBLE);
        addFriend.setVisibility(contact.hintpos >=0 ? View.GONE : View.VISIBLE);
        
        titleText.setText(contact.first_name +" " + contact.last_name);
        removeFromFriends.setVisibility(contact.hintpos >=0 ? View.VISIBLE : View.INVISIBLE);
        if(contact.photo_medium!=null) {
            contactImage.setImageFromURL(contact.photo_medium);
        } else if(contact.photo_bitmap!=null){
            contactImage.setImageBitmap(contact.photo_bitmap);
        } else {
            contactImage.setImageResource(R.drawable.im_photo_nophoto);
        }
    }
	
	private void serviceConnected()
	{
	    updateInfo(true);
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
