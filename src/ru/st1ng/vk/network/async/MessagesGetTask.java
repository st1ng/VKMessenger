package ru.st1ng.vk.network.async;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import ru.st1ng.vk.BuildConfig;
import ru.st1ng.vk.VKApplication;
import ru.st1ng.vk.model.Message;
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

public class MessagesGetTask extends BasicAsyncTask<Integer, Void, List<Message>> {


	public MessagesGetTask(AsyncCallback<List<Message>> callback) {
		super(callback);
	}

	@Override
	public String getMethodName() {
		return "messages.getHistory";
	}

	@Override
	public List<Message> parseResponse(String response)
			throws JsonParseException {
		if(BuildConfig.DEBUG)
			Log.d(VKApplication.TAG, "Got messages!");
		return JSONParser.parseGetMessagesResponse(response,VKApplication.getInstance().getCurrentUser().uid);	
	}

	@Override
	public void initNameValuePairs(ArrayList<NameValuePair> nameValuePairs,
			Integer... params) {
		nameValuePairs.add(new BasicNameValuePair("count", "" + params[0]));
		nameValuePairs.add(new BasicNameValuePair("offset", "" + params[1]));
		if(params[2]>0 || params[2] <=-200000000)
			nameValuePairs.add(new BasicNameValuePair("uid", "" + params[2]));
		else
			nameValuePairs.add(new BasicNameValuePair("chat_id", "" + (-params[2])));
		
	}
}
