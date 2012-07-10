package ru.st1ng.vk.activity;

import ru.st1ng.vk.R;
import ru.st1ng.vk.VKApplication;
import ru.st1ng.vk.network.ErrorCode;
import ru.st1ng.vk.network.async.AuthTask;
import ru.st1ng.vk.network.async.BasicAsyncTask.AsyncCallback;
import ru.st1ng.vk.network.async.SignupCheckPhoneTask;
import ru.st1ng.vk.network.async.SignupConfirmTask;
import ru.st1ng.vk.network.async.SignupTask;
import ru.st1ng.vk.util.FontsUtil;
import ru.st1ng.vk.util.UIUtil;
import ru.st1ng.vk.views.UserNameInputFilter;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class SignupActivity extends Activity{
	
	EditText phoneInput;
	EditText firstnameInput;
	EditText lastnameInput;
	Button signupButton;
	ImageView imageBack;
	TextView newAccText;
	
	SignupTask signupTask;
	
	EditText phoneInputConfirm;
	EditText passInput;
	EditText passInputConfirm;
	EditText codeInput;
	Button confirmButton;
	
	ImageView phoneOk;
	ImageView fnameOk;
	ImageView lnameOk;
	private BroadcastReceiver receiver;
	
	public static final String EXTRA_CODE = "code";
	public static final String EXTRA_NAME = "name";
	
	private String namePattern = "[[€-Ÿ]|[à-ß]|\\p{Alpha}|\\w]*";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_signup);
        
        
        phoneInput = (EditText) findViewById(R.id.phoneInput);
        firstnameInput = (EditText) findViewById(R.id.firstnameInput);
        lastnameInput = (EditText) findViewById(R.id.lastnameInput);
        signupButton = (Button) findViewById(R.id.signupButton);
        imageBack = (ImageView) findViewById(R.id.imageBack);
        newAccText = (TextView) findViewById(R.id.newAccText);
        phoneOk = (ImageView) findViewById(R.id.phoneOkImage);
        fnameOk = (ImageView) findViewById(R.id.fnameOkImage);
        lnameOk = (ImageView) findViewById(R.id.lnameOkImage);
        
        phoneInput.addTextChangedListener(allFilledWatcher);
        firstnameInput.addTextChangedListener(allFilledWatcher);
        lastnameInput.addTextChangedListener(allFilledWatcher);
        firstnameInput.addTextChangedListener(fnameCorrect);
        lastnameInput.addTextChangedListener(lnameCorrect);
        findViewById(R.id.frameBack).setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                SignupActivity.this.finish();
            }
        });
        firstnameInput.setFilters(new InputFilter[] { new UserNameInputFilter() });
        lastnameInput.setFilters(new InputFilter[] { new UserNameInputFilter() });
        
        phoneInput.setOnFocusChangeListener(new OnFocusChangeListener() {
            
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus) {
                    phoneOk.setVisibility(View.VISIBLE);
                    phoneOk.setImageResource(R.anim.ic_spin_animation_blue);
                    ((AnimationDrawable)phoneOk.getDrawable()).start();
                    new SignupCheckPhoneTask(new AsyncCallback<Boolean>() {

                        @Override
                        public void OnSuccess(Boolean str) {
                            phoneOk.setVisibility(View.VISIBLE);
                            phoneOk.setImageResource(R.drawable.ic_sent);                            
                        }

                        @Override
                        public void OnError(ErrorCode errorCode) {
                            UIUtil.showToast(SignupActivity.this, getString(errorCode.getStringResource()));
                            signupButton.setEnabled(false);
                            phoneOk.setVisibility(View.VISIBLE);
                            phoneOk.setImageResource(R.drawable.ic_error);
                        }
                        
                    }).execute(phoneInput.getText().toString());
                }
            }
        });
        phoneInput.setTypeface(FontsUtil.Helvetica);
        firstnameInput.setTypeface(FontsUtil.Helvetica);
        lastnameInput.setTypeface(FontsUtil.Helvetica);
        signupButton.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
        newAccText.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
        
        imageBack.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				onBackPressed();
			} 
		});
        phoneInput.requestFocus();
        
        signupButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				phoneInput.setEnabled(false);
				signupButton.setEnabled(false);
				signupTask = new SignupTask(new AsyncCallback<String>() {
					
					@Override
					public void OnSuccess(String str) {
						inflateConfirm();
						UIUtil.showToast(SignupActivity.this, getString(R.string.registration_sent));
					}
					
					@Override
					public void OnError(ErrorCode errorCode) {
						UIUtil.showToast(SignupActivity.this, getString(errorCode.getStringResource()));
						signupButton.setEnabled(true);
						phoneInput.setEnabled(true);
					}
				}, VKApplication.getInstance().getAPILang());
				signupTask.execute(phoneInput.getText().toString(), firstnameInput.getText().toString(), lastnameInput.getText().toString());

			}
		});
        
        IntentFilter filter = new IntentFilter();
        filter.addAction("ru.st1ng.vk.SMS_PARSE");

        receiver = new BroadcastReceiver() {
        	
			@Override
			public void onReceive(Context context, Intent intent) {
				String name = intent.getStringExtra(EXTRA_NAME);
				String code = intent.getStringExtra(EXTRA_CODE);
				if(codeInput!=null)
				{
					codeInput.setText(code);
					codeInput.setEnabled(false);
				}
			}
        };

        registerReceiver(receiver, filter);
    }
    
    private void inflateConfirm()
    {
    	View v = getLayoutInflater().inflate(R.layout.screen_signup_confirm, null);
    	newAccText.getRootView().startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
    	setContentView(v);
    	v.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
    	phoneInputConfirm = (EditText) findViewById(R.id.phoneInputConfirm);
    	passInput = (EditText) findViewById(R.id.passwordInput);
    	passInputConfirm = (EditText) findViewById(R.id.passwordInputConfirm);
    	codeInput = (EditText) findViewById(R.id.smsCode);
    	phoneInputConfirm.setText(phoneInput.getText().toString());
    	confirmButton = (Button) findViewById(R.id.confirmButton);
    	confirmButton.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
    	confirmButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(!passInput.getText().toString().equals(passInputConfirm.getText().toString()))
				{
					UIUtil.showToast(SignupActivity.this, getString(R.string.password_not_match));
					return;
				}
				confirmButton.setEnabled(false);
				new SignupConfirmTask(new AsyncCallback<Boolean>() {

					@Override
					public void OnSuccess(Boolean str) {
						if(str)
						{
							UIUtil.showToast(SignupActivity.this, getString(R.string.successfuly_registered));
							SignupActivity.this.finish();
						}
					}

					@Override
					public void OnError(ErrorCode errorCode) {
						UIUtil.showToast(SignupActivity.this, getString(errorCode.getStringResource()));
						confirmButton.setEnabled(true);
					}
				}, VKApplication.getInstance().getAPILang()).execute(phoneInputConfirm.getText().toString(), codeInput.getText().toString(), passInput.getText().toString());
			}
		});
    	passInput.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				confirmButton.setEnabled(s.length()>=6);
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				
			}
		});
    }
    
    protected void onDestroy() {
    	super.onDestroy();
    	unregisterReceiver(receiver);
    };
    
    
    TextWatcher allFilledWatcher = new TextWatcher() {
		
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			
		}
		
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			
		}
		
		@Override
		public void afterTextChanged(Editable s) {
			signupButton.setEnabled(!phoneInput.getText().toString().equals("") && !firstnameInput.getText().toString().equals("") && !lastnameInput.getText().toString().equals(""));
		}
	};
	
    TextWatcher fnameCorrect = new TextWatcher() {
        
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            
        }
        
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                int after) {
            
        }
        
        @Override
        public void afterTextChanged(Editable s) {
            if(s.toString().matches(namePattern)) {
                fnameOk.setVisibility(View.VISIBLE);
                fnameOk.setImageResource(R.drawable.ic_sent);                
            } else {
                signupButton.setEnabled(false);
                fnameOk.setVisibility(View.VISIBLE);
                fnameOk.setImageResource(R.drawable.ic_error);
            }
        }
    };
    
    TextWatcher lnameCorrect = new TextWatcher() {
        
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            
        }
        
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                int after) {
            
        }
        
        @Override
        public void afterTextChanged(Editable s) {
            if(s.toString().matches(namePattern)) {
                lnameOk.setVisibility(View.VISIBLE);
                lnameOk.setImageResource(R.drawable.ic_sent);                
            } else {
                signupButton.setEnabled(false);
                lnameOk.setVisibility(View.VISIBLE);
                lnameOk.setImageResource(R.drawable.ic_error);
            }
        }
    };
}