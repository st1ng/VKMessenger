package ru.st1ng.vk.network.async;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import ru.st1ng.vk.VKApplication;
import ru.st1ng.vk.data.ImageCache;
import ru.st1ng.vk.model.Message;
import ru.st1ng.vk.model.User;
import ru.st1ng.vk.model.VkAccount;
import ru.st1ng.vk.network.ErrorCode;
import ru.st1ng.vk.network.JSONParser;
import ru.st1ng.vk.network.JsonParseException;
import ru.st1ng.vk.util.HttpUtil;
import android.os.AsyncTask;
import android.os.Build;

/**
 * @author st1ng
 * AsyncTask for getting dialogs.
 * Execute params:
 * 0 - Dialog return count
 * 1 - Dialog return offset
 */

public class PushRegisterTask extends BasicAsyncTask<String, Void, Boolean> {


	public PushRegisterTask(AsyncCallback<Boolean> callback) {
		super(callback);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getMethodName() {
		return "account.registerDevice";
	}

	@Override
	public Boolean parseResponse(String response) throws JsonParseException {
		return true;
	}

	@Override
	public void initNameValuePairs(ArrayList<NameValuePair> nameValuePairs,
			String... params) {
		nameValuePairs.add(new BasicNameValuePair("token", params[0]));
		nameValuePairs.add(new BasicNameValuePair("device_model", "android"));
		nameValuePairs.add(new BasicNameValuePair("system_version", Build.VERSION.SDK_INT + ""));
		nameValuePairs.add(new BasicNameValuePair("no_text", "0"));
		
	}
}
