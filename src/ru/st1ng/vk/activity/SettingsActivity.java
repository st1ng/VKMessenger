package ru.st1ng.vk.activity;

import java.io.File;
import java.util.ArrayList;

import com.google.android.gcm.GCMRegistrar;

import ru.st1ng.vk.R;
import ru.st1ng.vk.VKApplication;
import ru.st1ng.vk.data.RecordsProvider;
import ru.st1ng.vk.model.Attachment;
import ru.st1ng.vk.model.GeoAttach;
import ru.st1ng.vk.model.PhotoAttach;
import ru.st1ng.vk.model.ServerUploadFile;
import ru.st1ng.vk.network.ErrorCode;
import ru.st1ng.vk.network.async.BasicAsyncTask.AsyncCallback;
import ru.st1ng.vk.network.async.GetProfileImageUploadServer;
import ru.st1ng.vk.network.async.SaveUploadProfileImageTask;
import ru.st1ng.vk.network.async.UploadProfilePhotoToServer;
import ru.st1ng.vk.network.async.UploadProfilePhotoToServer.UploadAsyncCallback;
import ru.st1ng.vk.util.FontsUtil;
import ru.st1ng.vk.util.ImageUtil;
import ru.st1ng.vk.util.SettingsUtil;
import ru.st1ng.vk.views.WebImageView;
import ru.st1ng.vk.views.WebImageView.ImageListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Spinner;
import android.widget.TextView;

public class SettingsActivity extends Activity {

	TextView title;
	CheckBox sendNotifications;
	CheckBox notificationSound;
	CheckBox notificationVibrate;
	
	Button logoff;
	Spinner showOnline;
	Spinner fontSize;
	TextView version;
	Button sendFeedback;
	Button contactAuthor;
	WebImageView profileImage;
	String photoOutput;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_settings);
		
		title = (TextView) findViewById(R.id.titleText);
		title.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
		version = (TextView) findViewById(R.id.version);
		sendFeedback = (Button) findViewById(R.id.sendFeedback);
		sendFeedback.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
		contactAuthor = (Button) findViewById(R.id.contactAuthor);
		contactAuthor.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
		profileImage = (WebImageView) findViewById(R.id.profileImage);
		
		notificationSound = (CheckBox) findViewById(R.id.notificationSound);
		notificationVibrate = (CheckBox) findViewById(R.id.notificationVibrate);
		if(VKApplication.getInstance().getCurrentUser().photo_medium!=null)
			profileImage.setImageFromURL(VKApplication.getInstance().getCurrentUser().photo_medium);

		profileImage.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				AlertDialog.Builder dialog = new AlertDialog.Builder(SettingsActivity.this);
				dialog.setItems(R.array.change_photo, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(SettingsActivity.this, ChangePhotoActivity.class);
						if(which==1)
							intent.putExtra(ChangePhotoActivity.EXTRA_TAKE_PHOTO, true);
						startActivity(intent);
					}
				}).show();
			}
		});
		try {
			version.setText("Version: " + getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
		} catch (NameNotFoundException e) {
			version.setVisibility(View.GONE);
		}
		
		showOnline = (Spinner) findViewById(R.id.showOnlineSpinner);
		fontSize = (Spinner) findViewById(R.id.fontSizesSpinner);
		showOnline.setSelection(SettingsUtil.getOnlineShowing(this));
		fontSize.setSelection(SettingsUtil.getFontSize(this));
		showOnline.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				SettingsUtil.setOnlineShowing(SettingsActivity.this, arg2);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		
		fontSize.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int pos, long arg3) {
				SettingsUtil.setFontSize(SettingsActivity.this, pos);
				FontsUtil.updateFontSize();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {			}
		});
		sendNotifications = (CheckBox) findViewById(R.id.sendNotificationsCB);
		logoff = (Button) findViewById(R.id.logoff);
		logoff.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
		findViewById(R.id.allowToSendNotifications).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				sendNotifications.performClick();
			}
		});
		sendNotifications.setChecked(SettingsUtil.isNotificationsEnabled(this));
		sendNotifications.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				SettingsUtil.setNotificationsEnabled(SettingsActivity.this, isChecked);
				notificationVibrate.setEnabled(isChecked);
				notificationSound.setEnabled(isChecked);
			}
		});
		
		notificationSound.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				SettingsUtil.setNotificationsSoundEnabled(SettingsActivity.this, isChecked);
			}
		});
		
		notificationVibrate.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				SettingsUtil.setNotificationsVibrateEnabled(SettingsActivity.this, isChecked);
			}
		});
		
		notificationSound.setEnabled(sendNotifications.isChecked());
		notificationVibrate.setEnabled(sendNotifications.isChecked());
		
		notificationSound.setChecked(SettingsUtil.isNotificationsSoundEnabled(this));
		notificationVibrate.setChecked(SettingsUtil.isNotificationsVibrateEnabled(this));
		logoff.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
		    	VKApplication.getInstance().logOff();
		    	GCMRegistrar.unregister(SettingsActivity.this);
		    	stopService(new Intent(SettingsActivity.this, RecordsProvider.class));
		    	startActivity(new Intent(SettingsActivity.this, LoginActivity.class));    	
		    	SettingsActivity.this.finish();    	

			}
		});
		
		sendFeedback.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent market = new Intent(Intent.ACTION_VIEW);
				market.setData(Uri.parse("market://details?id=ru.st1ng.vk"));
				try
				{
					startActivity(market);
				} catch (Exception e) {}
			}
		});
		
		contactAuthor.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				AlertDialog.Builder dialog = new AlertDialog.Builder(SettingsActivity.this);
				String[] items;
				boolean skypeInstalled = false;
				try
		        {
		               getPackageManager().getPackageInfo("com.skype.raider", PackageManager.GET_ACTIVITIES);
		               skypeInstalled = true;
		        }
		        catch (PackageManager.NameNotFoundException e)
		        {
		        	skypeInstalled = false;
		        }
				if(!skypeInstalled)
					items = new String[] { "Email", "Vkontakte" };
				else
					items = new String[] { "Email", "Vkontakte" };
				dialog.setItems(items, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent();
						switch(which)
						{
						case(0):
							intent.setAction(Intent.ACTION_SEND);
							intent.setType("text/plain");
							intent.setClassName("com.google.android.gm","com.google.android.gm.ComposeActivityGmail");
							intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"maratgalimzyanov@gmail.com"});
							intent.putExtra(Intent.EXTRA_SUBJECT, "Vk.com Messenger");
							break;
						case(1):
							intent.setClass(SettingsActivity.this, ConversationActivity.class);
							intent.putExtra(ConversationActivity.EXTRA_USERID, 16370391);
							break;
						case(2):
							intent.setAction(Intent.ACTION_VIEW);
							intent.setPackage("com.skype.raider");
							intent.setType("vnd.android.cursor.item/com.skype.android.chat.action");
							intent.setData(Uri.parse("purplecloud666"));
							break;
						}
						startActivity(intent);
					}
				});
				dialog.show();
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if(VKApplication.getInstance().getCurrentUser().photo_medium!=null)
			profileImage.setImageFromURL(VKApplication.getInstance().getCurrentUser().photo_medium);

	}
}
