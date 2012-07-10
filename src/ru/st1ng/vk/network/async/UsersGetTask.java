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

public class UsersGetTask extends BasicAsyncTask<Integer, Void, List<User>> {

	public UsersGetTask(AsyncCallback<List<User>> callback, String lang) {
		super(callback);
		this.lang = lang;
	}

	private String lang;
	

	@Override
	public String getMethodName() {
		return "users.get";
	}

	@Override
	public List<User> parseResponse(String response) throws JsonParseException {
		if(BuildConfig.DEBUG)
			Log.d(VKApplication.TAG, "Got users!");

		List<User> users = JSONParser.parseGetUsersResponse(response);
//		String photoDir = VKApplication.getInstance().getAvatarsDir();
//		for(User user : users)
//		{
//			if(ImageCache.getInstance().isPhotoPresentForUser(user, photoDir))
//			{
//				user.photo_bitmap = ImageCache.getInstance().getPhotoForUser(user, photoDir);
//			}
//		}
		return users;
	}

	@Override
	public void initNameValuePairs(ArrayList<NameValuePair> nameValuePairs,
			Integer... params) {
		StringBuilder uidBuilder = new StringBuilder();
		for(Integer id : params)
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
	}
}
