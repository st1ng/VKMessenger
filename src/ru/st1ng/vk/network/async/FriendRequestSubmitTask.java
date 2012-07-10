package ru.st1ng.vk.network.async;

import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import ru.st1ng.vk.model.Attachment;
import ru.st1ng.vk.model.GeoAttach;
import ru.st1ng.vk.model.Message;
import ru.st1ng.vk.network.JSONParser;
import ru.st1ng.vk.network.JsonParseException;

/**
 * @author st1ng
 * AsyncTask for getting dialogs.
 * Execute params:
 * 0 - Dialog return count
 * 1 - Dialog return offset
 */

public class FriendRequestSubmitTask extends BasicAsyncTask<Integer, Void, Boolean> {

	public FriendRequestSubmitTask(AsyncCallback<Boolean> callback) {
		super(callback);
	}

	@Override
	public String getMethodName() {
		return "friends.add";
	}

	@Override
	public Boolean parseResponse(String response) throws JsonParseException {
		return JSONParser.parseSuccessResponse(response);
	}

	@Override
	public void initNameValuePairs(ArrayList<NameValuePair> nameValuePairs,
			Integer... params) {
			nameValuePairs.add(new BasicNameValuePair("uid", params[0] +""));

	}
}
