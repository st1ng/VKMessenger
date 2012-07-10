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

public class MessagesByIdTask extends BasicAsyncTask<Integer, Void, List<Message>> {


	public MessagesByIdTask(AsyncCallback<List<Message>> callback) {
		super(callback);
	}

	@Override
	public String getMethodName() {
		return "messages.getById";
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
		StringBuilder uidBuilder = new StringBuilder();
		for(Integer id : params)
		{
			uidBuilder.append(String.valueOf(id));
			uidBuilder.append(",");
		}
		if(uidBuilder.length()>0)
			uidBuilder.deleteCharAt(uidBuilder.length()-1);

		nameValuePairs.add(new BasicNameValuePair("mids", uidBuilder.toString()));
		
	}
}
