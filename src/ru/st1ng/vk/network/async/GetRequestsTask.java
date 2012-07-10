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

public class GetRequestsTask extends BasicAsyncTask<Void, Void, List<User>> {

	public GetRequestsTask(AsyncCallback<List<User>> callback, String lang) {
		super(callback);
		this.lang = lang;
	}

	private String lang;
	

	@Override
	public String getMethodName() {
		return "friends.getRequests";
	}

	@Override
	public List<User> parseResponse(String response) throws JsonParseException {
		if(BuildConfig.DEBUG)
			Log.d(VKApplication.TAG, "Got friends!");
		List<Integer> requests = JSONParser.parseGetRequestsResponse(response);
		StringBuilder uidBuilder = new StringBuilder();

		ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("client_id", CLIENT_ID));
		nameValuePairs.add(new BasicNameValuePair("client_secret", CLIENT_SECRET));
		if(VKApplication.getInstance().getCurrentUser()==null)
		{
			errorCode = ErrorCode.UserNotLoggedIn;
//			return null;
		}
		else
		{
			nameValuePairs.add(new BasicNameValuePair("access_token", VKApplication.getInstance().getCurrentUser().token));
		}
		for(Integer id : requests)
		{
			uidBuilder.append(String.valueOf(id));
			uidBuilder.append(",");
		}
		if(uidBuilder.length()>0)
			uidBuilder.deleteCharAt(uidBuilder.length()-1);
		nameValuePairs.add(new BasicNameValuePair("uids", "" + uidBuilder.toString()));
		nameValuePairs.add(new BasicNameValuePair("fields", USER_FIELDS));
		if(lang!=null)
			nameValuePairs.add(new BasicNameValuePair("lang", lang));

		try {
			response = HttpUtil.getHttpSig(API_URL+"users.get", nameValuePairs,VKApplication.getInstance().getCurrentUser().secret);
			List<User> users = JSONParser.parseGetUsersResponse(response);
			String photoDir = VKApplication.getInstance().getAvatarsDir();
			
			for(User user : users)
			{
				user.request = true;
			}
			return users;
		} catch (Exception e) {
			return null;
		}

	}

	@Override
	public void initNameValuePairs(ArrayList<NameValuePair> nameValuePairs,
			Void... params) {
		nameValuePairs.add(new BasicNameValuePair("fields", USER_FIELDS));
		nameValuePairs.add(new BasicNameValuePair("count", "100"));
		if(lang!=null)
			nameValuePairs.add(new BasicNameValuePair("lang", lang));
	}
}
