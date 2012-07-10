package ru.st1ng.vk.network.async;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import ru.st1ng.vk.BuildConfig;
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
import android.util.Log;

/**
 * @author st1ng
 * AsyncTask for getting dialogs.
 * Execute params:
 * 0 - Dialog return count
 * 1 - Dialog return offset
 */

public class FriendsGetTask extends BasicAsyncTask<Integer, Void, List<User>> {

	public FriendsGetTask(AsyncCallback<List<User>> callback, String lang) {
		super(callback);
		this.lang = lang;
	}

	private String lang;
	

	@Override
	public String getMethodName() {
		return "friends.get";
	}

	@Override
	public List<User> parseResponse(String response) throws JsonParseException {
		if(BuildConfig.DEBUG)
			Log.d(VKApplication.TAG, "Got friends!");
		List<User> users = JSONParser.parseGetUsersResponse(response,true);
		String photoDir = VKApplication.getInstance().getAvatarsDir();
		
		return users;
	}

	@Override
	public void initNameValuePairs(ArrayList<NameValuePair> nameValuePairs,
			Integer... params) {
		nameValuePairs.add(new BasicNameValuePair("fields", USER_FIELDS));
		if(lang!=null)
			nameValuePairs.add(new BasicNameValuePair("lang", lang));
		nameValuePairs.add(new BasicNameValuePair("order", "hints"));
	}
}
