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

public class ChatRemoveUserTask extends BasicAsyncTask<String, Void, Boolean> {

	public ChatRemoveUserTask(AsyncCallback<Boolean> callback) {
		super(callback);
	}

	
	@Override
	public String getMethodName() {
		return "messages.removeChatUser";
	}

	@Override
	public Boolean parseResponse(String response) throws JsonParseException {
		return JSONParser.parseSuccessResponse(response);
	}

	@Override
	public void initNameValuePairs(ArrayList<NameValuePair> nameValuePairs,
			String... params) {
			nameValuePairs.add(new BasicNameValuePair("chat_id", params[0]));
			nameValuePairs.add(new BasicNameValuePair("uid", params[1]));
	}
}
