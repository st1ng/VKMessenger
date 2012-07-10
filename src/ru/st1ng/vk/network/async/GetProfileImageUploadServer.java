package ru.st1ng.vk.network.async;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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

/**
 * @author st1ng
 * AsyncTask for getting dialogs.
 * Execute params:
 * 0 - Dialog return count
 * 1 - Dialog return offset
 */

public class GetProfileImageUploadServer extends BasicAsyncTask<Void, Void, String> {

	public GetProfileImageUploadServer(AsyncCallback<String> callback) {
		super(callback);
	}

	@Override
	public String getMethodName() {
		return "photos.getProfileUploadServer";
	}

	@Override
	public String parseResponse(String response) throws JsonParseException {
		
		return JSONParser.parseMessagesUploadServerResponse(response);
	}

	@Override
	public void initNameValuePairs(ArrayList<NameValuePair> nameValuePairs,
			Void... params) {
	}
}
