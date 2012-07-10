package ru.st1ng.vk.network.async;

import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import ru.st1ng.vk.model.VkAccount;
import ru.st1ng.vk.network.ErrorCode;
import ru.st1ng.vk.network.JSONParser;
import ru.st1ng.vk.network.JsonParseException;
import ru.st1ng.vk.util.HttpUtil;


/**
 * @author st1ng
 * AsyncTask for authenticate the user.
 * Execute params:
 * 0 - Username
 * 1 - Password
 */
public class AuthTask extends BasicAsyncTask<String, Void, VkAccount> {

	private static final String PATH = "oauth/token";
	
	public AuthTask(AsyncCallback<VkAccount> handler)
	{
		super(handler);
	}
	
	@Override
	protected VkAccount doInBackground(String... params) {
		VkAccount result;
		errorCode = ErrorCode.NoError;
		ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("grant_type", "password"));
		nameValuePairs.add(new BasicNameValuePair("client_id", CLIENT_ID));
		nameValuePairs.add(new BasicNameValuePair("client_secret", CLIENT_SECRET));
		nameValuePairs.add(new BasicNameValuePair("username", params[0]));
		nameValuePairs.add(new BasicNameValuePair("password", params[1]));
		nameValuePairs.add(new BasicNameValuePair("scope", SCOPE));

		String response = HttpUtil.getHttps(API_URL_AUTH+PATH, nameValuePairs);
		if(response==null)
		{
			errorCode = ErrorCode.NetworkUnavailable;
			return null;
		}
		try
		{
			result = JSONParser.parseAuthResponse(response);
			result.username = params[0];
			return result;
		}
		catch(JsonParseException e)
		{
			errorCode = e.getErrorCode();
			return null;
		}
		
	}

	@Override
	public String getMethodName() {
		return null;
	}

	@Override
	public VkAccount parseResponse(String response) {
		return null;
	}

	@Override
	public void initNameValuePairs(ArrayList<NameValuePair> nameValuePairs,
			String... params) {
	}
}
