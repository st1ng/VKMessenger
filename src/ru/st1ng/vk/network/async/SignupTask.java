package ru.st1ng.vk.network.async;

import java.util.ArrayList;
import java.util.EnumSet;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import ru.st1ng.vk.VKApplication;
import ru.st1ng.vk.model.VkAccount;
import ru.st1ng.vk.network.ErrorCode;
import ru.st1ng.vk.network.JSONParser;
import ru.st1ng.vk.network.JsonParseException;
import ru.st1ng.vk.util.HttpUtil;
import android.os.AsyncTask;


public class SignupTask extends BasicAsyncTask<String, Void, String> {

	
	public SignupTask(AsyncCallback<String> callback, String lang) {
		super(callback);
		this.lang = lang;
		useHttps = true;
	}

	private String lang;
	
	@Override
	public String getMethodName() {
		return "auth.signup";
	}

	@Override
	public String parseResponse(String response) throws JsonParseException {
		return JSONParser.parseSignupResponse(response);
	}

	@Override
	public void initNameValuePairs(ArrayList<NameValuePair> nameValuePairs,
			String... params) {
		nameValuePairs.add(new BasicNameValuePair("phone", params[0]));
		nameValuePairs.add(new BasicNameValuePair("first_name", params[1]));
		nameValuePairs.add(new BasicNameValuePair("last_name", params[2]));
		if(params.length>3)
			nameValuePairs.add(new BasicNameValuePair("sid", params[4]));
		if(lang!=null)
			nameValuePairs.add(new BasicNameValuePair("lang", lang));
		
		errorCode = ErrorCode.NoError;
	}
}
