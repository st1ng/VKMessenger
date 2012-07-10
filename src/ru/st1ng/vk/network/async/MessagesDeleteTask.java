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

public class MessagesDeleteTask extends BasicAsyncTask<Integer, Void, Boolean> {

	public MessagesDeleteTask(AsyncCallback<Boolean> callback) {
		super(callback);
	}

	
	@Override
	public String getMethodName() {
		return "messages.delete";
	}

	@Override
	public Boolean parseResponse(String response) throws JsonParseException {
		return JSONParser.parseSuccessMessageDeleteResponse(response);
	}

	@Override
	public void initNameValuePairs(ArrayList<NameValuePair> nameValuePairs,
			Integer... params) {
			StringBuilder mids = new StringBuilder();
			for(int mid : params)
			{
				mids.append(mid +",");
			}
			if(mids.length()>0)
				mids.deleteCharAt(mids.length()-1);
			nameValuePairs.add(new BasicNameValuePair("mids", mids.toString()));
	}
}
