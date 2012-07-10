package ru.st1ng.vk.activity;

import ru.st1ng.vk.R;
import ru.st1ng.vk.data.RecordsProvider;
import ru.st1ng.vk.data.RecordsProvider.LocalBinder;
import ru.st1ng.vk.data.RecordsProvider.RecordsWatcher;
import ru.st1ng.vk.model.ContactName;
import ru.st1ng.vk.network.ErrorCode;
import ru.st1ng.vk.util.FontsUtil;
import ru.st1ng.vk.util.UIUtil;
import ru.st1ng.vk.views.WebImageView;
import ru.st1ng.vk.views.adapter.AllUsersListAdapter;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
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
import android.widget.ListView;
import android.widget.TextView;

public class ContactInfoActivity extends Activity {
	
	private boolean mBound = false;
	RecordsProvider recordsManager;
	
	int contactId;
	
	TextView titleText;
	Button sendMessage;
	Button call;
	WebImageView contactImage;
	public static final String EXTRA_CONTACTID = "contactid";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_contactinfo);
		contactId = getIntent().getIntExtra(EXTRA_CONTACTID, -1);
		titleText = (TextView) findViewById(R.id.titleText);
		sendMessage = (Button) findViewById(R.id.buttonSendMessage);
		call = (Button) findViewById(R.id.buttonCall);
		contactImage = (WebImageView) findViewById(R.id.contactImage);
		titleText.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
		call.setTypeface(FontsUtil.Helvetica);
		sendMessage.setTypeface(FontsUtil.Helvetica);
		
		findViewById(R.id.frameBack).setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
	}
	
	
	private void serviceConnected()
	{
		final ContactName contact = recordsManager.getContactById(contactId);
		if(contact==null)
		{
			UIUtil.showToast(this, "Contact not found");
			this.finish();
		}
		titleText.setText(contact.contact_name);
		if(contact.phones!=null)
			call.setText(getString(R.string.call) + " " + contact.phones);
		if(contact.photo_medium!=null) {
		    contactImage.setImageFromURL(contact.photo_medium);
		} else if(contact.photo_bitmap!=null){
		    contactImage.setImageBitmap(contact.photo_bitmap);
		} else {
		    contactImage.setImageResource(R.drawable.im_photo_nophoto);
		}
		call.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_DIAL);
				intent.setData(Uri.parse("tel:"+contact.phones));
				startActivity(intent);
			}
		});
		sendMessage.setText(contact.uid>0 ? R.string.send_message : R.string.send_invintation);
		findViewById(R.id.userNotRegisteredText).setVisibility(contact.uid>0 ? View.GONE : View.VISIBLE);
//		sendMessage.setVisibility(contact.uid>0 ? View.VISIBLE : View.GONE);
		sendMessage.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
			    if(contact.uid>0) {
    				Intent intent = new Intent(ContactInfoActivity.this, ConversationActivity.class);
    				intent.putExtra(ConversationActivity.EXTRA_USERID, contact.uid);
    				startActivity(intent);
			    } else {
			        String smsBody=getString(R.string.invintation_text);
			        Intent sendIntent = new Intent(Intent.ACTION_VIEW);
			        sendIntent.putExtra("sms_body", smsBody); 
			        sendIntent.putExtra("address", contact.phones);
			        sendIntent.setType("vnd.android-dir/mms-sms");
			        startActivity(sendIntent);			    }
			}
		});
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
}
