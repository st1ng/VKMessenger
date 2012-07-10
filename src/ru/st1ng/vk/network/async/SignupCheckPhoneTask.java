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


public class SignupCheckPhoneTask extends BasicAsyncTask<String, Void, Boolean> {

	
	public SignupCheckPhoneTask(AsyncCallback<Boolean> callback) {
		super(callback);
		useHttps = true;
	}

	@Override
	public String getMethodName() {
		return "auth.checkPhone";
	}

	@Override
	public Boolean parseResponse(String response) throws JsonParseException {
		return JSONParser.parseCheckPhoneResponse(response);
	}

	@Override
	public void initNameValuePairs(ArrayList<NameValuePair> nameValuePairs,
			String... params) {
		nameValuePairs.add(new BasicNameValuePair("phone", params[0]));
		errorCode = ErrorCode.NoError;
	}
}
