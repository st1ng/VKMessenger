package ru.st1ng.vk.activity;

import java.util.ArrayList;
import java.util.List;

import ru.st1ng.vk.R;
import ru.st1ng.vk.VKApplication;
import ru.st1ng.vk.data.ImageCache;
import ru.st1ng.vk.data.RecordsProvider;
import ru.st1ng.vk.model.User;
import ru.st1ng.vk.model.VkAccount;
import ru.st1ng.vk.network.ErrorCode;
import ru.st1ng.vk.network.async.AuthTask;
import ru.st1ng.vk.network.async.BasicAsyncTask.AsyncCallback;
import ru.st1ng.vk.network.async.DownloadImagesTask;
import ru.st1ng.vk.network.async.UsersGetTask;
import ru.st1ng.vk.util.FontsUtil;
import ru.st1ng.vk.util.SystemUtil;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class LoginActivity extends Activity {
	
	TextView signUpTV;
	TextView signUpUnderTV;
	AutoCompleteTextView phoneInput;
	EditText passInput;
	ImageView phoneImage;
	ImageView passImage;
	ImageView progressImage;
	AnimationDrawable progressAnimation;
	
	Button loginButton;
	View signupButton;
	
	
	AuthTask authTask;
	
	ArrayList<VkAccount> usersLoggedInBefore; //For auto-completion of phoneInput field.
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        usersLoggedInBefore = VKApplication.getDatabase().getAccounts();
        if(usersLoggedInBefore.size()>0 && !usersLoggedInBefore.get(0).token.equals(""))
        {
        	VKApplication.getInstance().setCurrentUser(usersLoggedInBefore.get(0));
        	VKApplication.getInstance().runUpdateUser(usersLoggedInBefore.get(0));
			if(!SystemUtil.isMyServiceRunning(LoginActivity.this))
				startService(new Intent(LoginActivity.this, RecordsProvider.class));
			startActivity(new Intent(LoginActivity.this, TabhostActivity.class));
        	this.finish();
        	return;
        }
        setContentView(R.layout.screen_login);
        

		/*
         * Damn :( VK official app doesn't implement getAuthToken
         * So this code doesn't work now.
         * Maybe will work later if I win contest :)
        AccountManager am = AccountManager.get(this);
        Account[] accounts = am.getAccounts();
        for(Account acc : accounts)
        {
        	if(acc.type.equals("com.vkontakte.account"))
        	{
	        	Bundle options = new Bundle();
	        	//This is a test key. Register your own at http://www.last.fm/api
	        	options.putString("client_id", BasicAsyncTask.CLIENT_ID);
	        	options.putString("client_secret", BasicAsyncTask.CLIENT_SECRET);
	        	am.getAuthToken(accounts[0], "", options, LoginActivity.this, new AccountManagerCallback<Bundle>() {
					public void run(AccountManagerFuture<Bundle> arg0) {
						try {
							Bundle bundle = arg0.getResult();
							String key = bundle.getString(AccountManager.KEY_AUTHTOKEN);
							key = "";
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
	        	}, null);
        	}
        }
        */
        

        signUpTV = (TextView) findViewById(R.id.signupRegisterText);
        signUpUnderTV = (TextView) findViewById(R.id.signupUnderText);
        
        phoneInput = (AutoCompleteTextView) findViewById(R.id.phoneInput);
        passInput = (EditText) findViewById(R.id.passInput);
        
        phoneImage = (ImageView) findViewById(R.id.phoneImage);
        passImage = (ImageView) findViewById(R.id.passImage);
        progressImage = (ImageView) findViewById(R.id.progressImage);
        progressAnimation = (AnimationDrawable) progressImage.getDrawable();
        
        loginButton = (Button) findViewById(R.id.loginButton);
        signupButton = findViewById(R.id.signupFrame);

        
        phoneInput.setAdapter(new ArrayAdapter<VkAccount>(LoginActivity.this, R.layout.item_autocomplete_list, usersLoggedInBefore));
        phoneInput.setDropDownBackgroundResource(R.drawable.invisible);
        phoneInput.setOnFocusChangeListener(new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				phoneImage.setImageResource(hasFocus ? R.drawable.im_phone_active : R.drawable.im_phone);
			}
		});
        phoneInput.setOnEditorActionListener(new OnEditorActionListener() {
			
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				passInput.requestFocus();
				return true;
			}
		});
        
        passInput.setOnFocusChangeListener(new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				passImage.setImageResource(hasFocus ? R.drawable.im_pass_active : R.drawable.im_pass);
			}
		});
        
        passInput.setOnEditorActionListener(new OnEditorActionListener() {
			
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				loginButton.performClick();
				return true;
			}
		});

        signupButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startActivity(new Intent(LoginActivity.this, SignupActivity.class));
			}
		});
        
        loginButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
		        authTask = new AuthTask(new AsyncCallback<VkAccount>() {
					
					@Override
					public void OnSuccess(VkAccount user) {
						VKApplication.getDatabase().addOrReplaceUser(user);
						VKApplication.getInstance().setCurrentUser(user);
						VKApplication.getInstance().runUpdateUser(user);
						if(!SystemUtil.isMyServiceRunning(LoginActivity.this))
							startService(new Intent(LoginActivity.this, RecordsProvider.class));
			        	startActivity(new Intent(LoginActivity.this, TabhostActivity.class));
			        	LoginActivity.this.finish();
					}
					
					@Override
					public void OnError(ErrorCode errorCode) {
						Toast.makeText(LoginActivity.this, getString(errorCode.getStringResource()), Toast.LENGTH_LONG).show();
						loginButton.setEnabled(true);
						progressAnimation.stop();
						progressImage.setVisibility(View.INVISIBLE);
						authTask=null;

					}
				});
				loginButton.setEnabled(false);
				progressImage.setVisibility(View.VISIBLE);
				progressAnimation.start();
				authTask.execute(phoneInput.getText().toString(), passInput.getText().toString());				
			}
		});
        
        phoneInput.addTextChangedListener(allFilledWatcher);
        passInput.addTextChangedListener(allFilledWatcher);
        phoneInput.setTypeface(FontsUtil.Helvetica);
        passInput.setTypeface(FontsUtil.Helvetica);
        loginButton.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
        signUpTV.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
        signUpUnderTV.setTypeface(FontsUtil.Helvetica);
        TextView signupRegisterText  = (TextView) findViewById(R.id.signupRegisterText);
        
        
    }
    
    

    
    private TextWatcher allFilledWatcher = new TextWatcher() {
		
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			
		}
		
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			
		}
		
		@Override
		public void afterTextChanged(Editable s) {
			loginButton.setEnabled(!phoneInput.getText().toString().equals("") && !passInput.getText().toString().equals(""));
		}
	};	
}