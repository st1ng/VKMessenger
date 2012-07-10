package ru.st1ng.vk.network.async;

import java.util.ArrayList;
import java.util.EnumSet;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import ru.st1ng.vk.VKApplication;
import ru.st1ng.vk.model.VkAccount;
import ru.st1ng.vk.network.ErrorCode;
import ru.st1ng.vk.network.JSONParser;
import ru.st1ng.vk.network.JsonParseException;
import ru.st1ng.vk.util.HttpUtil;
import android.os.AsyncTask;


public class ImportContactsTask extends BasicAsyncTask<String, Void, String> {

	
	public ImportContactsTask(AsyncCallback<String> callback) {
		super(callback);
	}

	private String lang;
	
	@Override
	public String getMethodName() {
		return "account.importContacts";
	}

	@Override
	public String parseResponse(String response) throws JsonParseException {
		return "";
//		return JSONParser.parseSignupResponse(response);
	}

	@Override
	public void initNameValuePairs(ArrayList<NameValuePair> nameValuePairs,
			String... params) {
		StringBuilder uidBuilder = new StringBuilder();
		for(String id : params)
		{
			uidBuilder.append(id);
			uidBuilder.append(",");
		}
		if(uidBuilder.length()>0)
			uidBuilder.deleteCharAt(uidBuilder.length()-1);
		String uids = uidBuilder.toString();
		uids = uids.replace(" ", "");
		uids = uids.replace("-", "");
		uids = uids.replace("+", "");
		nameValuePairs.add(new BasicNameValuePair("contacts", uids));
		
	}
}
