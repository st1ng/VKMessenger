package ru.st1ng.vk.network.async;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
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

public class VideoGetTask extends BasicAsyncTask<Integer, Void, HashMap<Integer,String>> {


	public VideoGetTask(AsyncCallback<HashMap<Integer,String>> callback) {
		super(callback);
	}

	@Override
	public String getMethodName() {
		return "video.get";
	}

	@Override
	public HashMap<Integer,String> parseResponse(String response) throws JsonParseException {
		if(BuildConfig.DEBUG)
			Log.d(VKApplication.TAG, "Got video info!");
		return JSONParser.parseVideoGetResponse(response);
	}

	@Override
	public void initNameValuePairs(ArrayList<NameValuePair> pairs, Integer... params) {
		pairs.add(new BasicNameValuePair("videos", params[0] + "_" + params[1]));
	}
	
}
