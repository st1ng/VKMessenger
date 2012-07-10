package ru.st1ng.vk.activity;

import java.util.List;

import ru.st1ng.vk.R;
import ru.st1ng.vk.VKApplication;
import ru.st1ng.vk.data.RecordsProvider;
import ru.st1ng.vk.data.RecordsProvider.LocalBinder;
import ru.st1ng.vk.data.RecordsProvider.RecordsWatcher;
import ru.st1ng.vk.model.ContactName;
import ru.st1ng.vk.model.Message;
import ru.st1ng.vk.model.User;
import ru.st1ng.vk.network.ErrorCode;
import ru.st1ng.vk.network.async.BasicAsyncTask.AsyncCallback;
import ru.st1ng.vk.network.async.ChatAddUserTask;
import ru.st1ng.vk.network.async.ChatEditTask;
import ru.st1ng.vk.network.async.ChatGetTask;
import ru.st1ng.vk.network.async.ChatRemoveUserTask;
import ru.st1ng.vk.util.FontsUtil;
import ru.st1ng.vk.util.UIUtil;
import ru.st1ng.vk.views.UserImageView;
import ru.st1ng.vk.views.adapter.AllUsersListAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class GroupInfoActivity extends Activity {
	
	private boolean mBound = false;
	RecordsProvider recordsManager;
	RecordsWatcher recordsWatcher;
	int chatId;
	
	TextView titleText;
	
	Message currentChat;
	LinearLayout groupUsersLayout;
	
	Button buttonEdit;
	Button buttonAddPerson;
	Button buttonLeave;
	EditText chatName;
	public static final String EXTRA_CHATID = "chatid";
	public static final String EXTRA_LEAVE = "leave";
	private String prevChatName;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_groupinfo);
		chatId = getIntent().getIntExtra(EXTRA_CHATID, -1);
		titleText = (TextView) findViewById(R.id.titleText);
		titleText.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
		groupUsersLayout = (LinearLayout) findViewById(R.id.groupUsersLayout);
		buttonEdit = (Button) findViewById(R.id.buttonChangeChat);
		buttonEdit.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
		chatName = (EditText) findViewById(R.id.textChat);
		chatName.setTypeface(FontsUtil.Helvetica);
		buttonAddPerson = (Button) findViewById(R.id.buttonAddPerson);
		buttonAddPerson.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
		buttonAddPerson.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(GroupInfoActivity.this, ComposeMessageActivity.class);
				intent.putExtra(ComposeMessageActivity.EXTRA_CHOOSEMODE, true);
				startActivityForResult(intent, 0);
			}
		});
		buttonLeave = (Button) findViewById(R.id.buttonLeaveChat);
		buttonLeave.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
		buttonLeave.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				AlertDialog.Builder dialog = new AlertDialog.Builder(GroupInfoActivity.this);
				dialog.setMessage(R.string.are_you_sure_you_want_to_leave).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						new ChatRemoveUserTask(new AsyncCallback<Boolean>() {

							@Override
							public void OnSuccess(Boolean str) {
								if(mBound)
								{
									recordsManager.deleteDialogs(chatId);
								}
							}

							@Override
							public void OnError(ErrorCode errorCode) {
								UIUtil.showToast(GroupInfoActivity.this, getString(errorCode.getStringResource()));
							}
						}).execute(String.valueOf(-chatId), String.valueOf(VKApplication.getInstance().getCurrentUser().uid));
					}
				}).setNegativeButton(android.R.string.no, null);
				dialog.show();
			}
		});
		chatName.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				if(prevChatName!=null && !s.toString().equals(prevChatName))
				{
					buttonEdit.setEnabled(true);
				}
				else
				{
					buttonEdit.setEnabled(false);
				}
			}
		});
		buttonEdit.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				chatName.setEnabled(false);
				new ChatEditTask(new AsyncCallback<Boolean>() {

					@Override
					public void OnSuccess(Boolean str) {
						runOnUiThread(new Runnable() {
							
							@Override
							public void run() {
								chatName.setEnabled(true);
								prevChatName = chatName.getText().toString();
								buttonEdit.setEnabled(false);
								UIUtil.showToast(GroupInfoActivity.this, getString(R.string.chat_changed_success));
							}
						});
					}

					@Override
					public void OnError(final ErrorCode errorCode) {
						runOnUiThread(new Runnable() {
							
							@Override
							public void run() {
								Toast.makeText(GroupInfoActivity.this, getString(errorCode.getStringResource()), Toast.LENGTH_LONG).show();
								chatName.setEnabled(true);
							}
						});
					}
				}).execute("" + (-chatId), chatName.getText().toString());
			}
		});
		recordsWatcher = new RecordsWatcher() {
			
			@Override
			public void OnError(ErrorCode errorCode) {
				if(errorCode.equals(ErrorCode.MessagesSuccessfullyDeleted))
				{
					Intent result = new Intent();
					result.putExtra(EXTRA_LEAVE, true);
					setResult(RESULT_OK, result);
					GroupInfoActivity.this.finish();
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
		
		findViewById(R.id.backLayout).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});
	}
	
	private void updateInfo(boolean init)
	{
		groupUsersLayout.removeAllViews();
		currentChat = recordsManager.getChatById(chatId);
		if(init)
		{
			new ChatGetTask(new AsyncCallback<Message>() {
	
				@Override
				public void OnSuccess(Message str) {
					str.chat_id = -chatId;
					if(mBound)
					{
						recordsManager.addOrReplaceChat(str);
					}
					currentChat = str;				
					updateInfo(false);
				}
	
				@Override
				public void OnError(ErrorCode errorCode) {
					UIUtil.showToast(GroupInfoActivity.this, getString(errorCode.getStringResource()));
				}
			}).execute(-chatId);
		}
		if(chatName.getText().toString().length()==0 && currentChat.title!=null)
		{
			chatName.setText(currentChat.title);
			prevChatName = currentChat.title;
		}
		if(currentChat.chat_active!=null)
		    currentChat.chat_active.replace("" + VKApplication.getInstance().getCurrentUser().uid, "");
		String[] usersStr = currentChat.chat_active.split(",");
		
		Integer[] usersList = new Integer[usersStr.length];
		for(int i = 0;i<usersStr.length;i++)
		{
			try {
				usersList[i] = Integer.parseInt(usersStr[i]);
			} catch (Exception e){
				
			}
		}
		List<User> users = recordsManager.getUsersById(usersList, true);
		titleText.setText(usersStr.length-1 + " " +getString(R.string.active_users));
		for(User user : users)
		{
			if(user.uid==VKApplication.getInstance().getCurrentUser().uid)
				continue;
			final View userView = getLayoutInflater().inflate(R.layout.item_groupuser_list, null);
			
			groupUsersLayout.addView(userView);
			((UserImageView)userView.findViewById(R.id.dialogIcon)).setUser(user);
			((TextView)userView.findViewById(R.id.dialogTitle)).setText(user.first_name + " " + user.last_name);
			((TextView)userView.findViewById(R.id.dialogTitle)).setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
			((ImageView)userView.findViewById(R.id.dialogOnline)).setVisibility(user.online ? View.VISIBLE : View.INVISIBLE);
			final int uid = user.uid;
			userView.findViewById(R.id.imageDelete).setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					new ChatRemoveUserTask(new AsyncCallback<Boolean>() {

						@Override
						public void OnSuccess(Boolean str) {
							UIUtil.showToast(GroupInfoActivity.this, getString(R.string.successfuly_remove_user));
							userView.setVisibility(View.GONE);
							updateInfo(true);
						}

						@Override
						public void OnError(ErrorCode errorCode) {
							UIUtil.showToast(GroupInfoActivity.this, getString(errorCode.getStringResource()));
						}
					}).execute(String.valueOf(-chatId), String.valueOf(uid));
				}
			});
		}
	}
	
	private void serviceConnected()
	{
		updateInfo(true);
		recordsManager.addWatcher(recordsWatcher);
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
    		
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
     
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if(requestCode==0 && resultCode==RESULT_OK)
    	{
    		int uid = data.getIntExtra(ComposeMessageActivity.EXTRA_RESULT_USERID, 0);
    		if(uid==0)
    			return;
    		new ChatAddUserTask(new AsyncCallback<Boolean>() {

				@Override
				public void OnSuccess(Boolean str) {
					runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
							UIUtil.showToast(GroupInfoActivity.this, getString(R.string.successfuly_submited));
							if(mBound)
								updateInfo(true);
						}
					});
				}

				@Override
				public void OnError(ErrorCode errorCode) {
					UIUtil.showToast(GroupInfoActivity.this, getString(errorCode.getStringResource()));					
				}
    			
			}).execute(String.valueOf(-chatId), String.valueOf(uid));
    	}
    };
}
