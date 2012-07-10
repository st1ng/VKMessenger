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

public class ChatGetTask extends BasicAsyncTask<Integer, Void, Message> {


	public ChatGetTask(AsyncCallback<Message> callback) {
		super(callback);
	}

	@Override
	public String getMethodName() {
		return "messages.getChat";
	}

	@Override
	public Message parseResponse(String response)
			throws JsonParseException {
		return JSONParser.parseGetChatResponse(response);	
	}

	@Override
	public void initNameValuePairs(ArrayList<NameValuePair> nameValuePairs,
			Integer... params) {
			nameValuePairs.add(new BasicNameValuePair("chat_id", "" + (params[0])));
		
	}
}
