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


public class SignupConfirmTask extends BasicAsyncTask<String, Void, Boolean> {

	
	public SignupConfirmTask(AsyncCallback<Boolean> callback, String lang) {
		super(callback);
		this.lang = lang;
		useHttps = true;
	}

	private String lang;
	
	@Override
	public String getMethodName() {
		return "auth.confirm";
	}

	@Override
	public Boolean parseResponse(String response) throws JsonParseException {
		return JSONParser.parseSignupConfirmResponse(response);
	}

	@Override
	public void initNameValuePairs(ArrayList<NameValuePair> nameValuePairs,
			String... params) {
		nameValuePairs.add(new BasicNameValuePair("phone", params[0]));
		nameValuePairs.add(new BasicNameValuePair("code", params[1]));
		nameValuePairs.add(new BasicNameValuePair("password", params[2]));
		if(params.length>3)
			nameValuePairs.add(new BasicNameValuePair("sid", params[4]));
		if(lang!=null)
			nameValuePairs.add(new BasicNameValuePair("lang", lang));
		
		errorCode = ErrorCode.NoError;
	}
}
