package ru.st1ng.vk.network.async;

import java.util.ArrayList;
import java.util.Random;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import ru.st1ng.vk.VKApplication;
import ru.st1ng.vk.model.Attachment;
import ru.st1ng.vk.model.GeoAttach;
import ru.st1ng.vk.model.Message;
import ru.st1ng.vk.network.ErrorCode;
import ru.st1ng.vk.network.JSONParser;
import ru.st1ng.vk.network.JsonParseException;
import ru.st1ng.vk.util.HttpUtil;

/**
 * @author st1ng
 * AsyncTask for getting dialogs.
 * Execute params:
 * 0 - Dialog return count
 * 1 - Dialog return offset
 */

public class SendMessageTask extends BasicAsyncTask<String, Void, Message> {

	private String forwardedMessages;
	private String coordinates;
	private String guid;
	private JsonParseException exception;
	SendAsyncCallback<Message> callback;
	private String captchaSid;
	private String captchaKey;

       public SendMessageTask(SendAsyncCallback<Message> callback, String forwardedMessages, String coordinates, String guid,String captchaSid, String captchaKey) {
            super(callback);
            this.callback = callback;
            this.forwardedMessages = forwardedMessages;
            this.coordinates = coordinates;
            this.setGuid(guid);
            this.captchaKey = captchaKey;
            this.captchaSid = captchaKey;
        }

	private int conversationId;
	private String body;
	
	@Override
	public String getMethodName() {
		return "messages.send";
	}

	   protected Message doInBackground(String... params) {
	        errorCode = ErrorCode.NoError;
	        ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
	        nameValuePairs.add(new BasicNameValuePair("client_id", CLIENT_ID));
	        nameValuePairs.add(new BasicNameValuePair("client_secret", CLIENT_SECRET));
	        if(VKApplication.getInstance().getCurrentUser()==null)
	        {
	            errorCode = ErrorCode.UserNotLoggedIn;
//	          return null;
	        }
	        else
	        {
	            nameValuePairs.add(new BasicNameValuePair("access_token", VKApplication.getInstance().getCurrentUser().token));
	        }
	        initNameValuePairs(nameValuePairs,params);

	        String response;
	        try {
	            if(!useHttps && !useHttpsSig)
	                response = HttpUtil.getHttpSig(API_URL+getMethodName(), nameValuePairs,VKApplication.getInstance().getCurrentUser().secret);
	            else if(useHttpsSig)
	                response = HttpUtil.getHttpsSig(API_URL_HTTPS+getMethodName(), nameValuePairs,VKApplication.getInstance().getCurrentUser().secret);
	            else
	                response = HttpUtil.getHttps(API_URL_HTTPS+getMethodName(), nameValuePairs);
	        } catch (Exception e1) {
	            attempts++;
	            errorCode = ErrorCode.NetworkUnavailable;
	            
	            if(attempts<3)
	            {
	                if(attempts==1)
	                    publishProgress();
	                try {
	                    Thread.sleep(3000);
	                } catch (InterruptedException e) {
	                }
	                return doInBackground(params);
	            }
	            
	            return null;
	        }
	        attempts = 0;
	        try {
	            return parseResponse(response);
	        } catch (JsonParseException e) {
	            exception = e;
	            return null;
	        }
	        
	    };
	    
	@Override
	public Message parseResponse(String response) throws JsonParseException {
		Message result = new Message();
        if(conversationId>0 || conversationId<=-200000000)
            result.uid = conversationId;
        else
            result.chat_id = -conversationId;
        result.body = body;
        result.date = 0;//System.currentTimeMillis()/1000;
        result.out = true;
        result.pendingSend = true;
        result.sent = true;
        result.guid = getGuid();
		result.mid = JSONParser.parseSendMessageResponse(response,result);
		return result;
	}

	@Override
	public void initNameValuePairs(ArrayList<NameValuePair> nameValuePairs,
			String... params) {
		String encodedMessage = params[0];
		/*encodedMessage = encodedMessage.replace("+", "PLUSSIGN"); 
		encodedMessage = URLEncoder.encode(encodedMessage);
		encodedMessage = encodedMessage.replace("*", "%2A");  
		encodedMessage = encodedMessage.replace("~", "%7E");  
		encodedMessage = encodedMessage.replace("PLUSSIGN", "%2B");*/
		if(encodedMessage.length()>0)
			nameValuePairs.add(new BasicNameValuePair("message", encodedMessage));
		if(params.length>2)
		{
			StringBuilder idBuilder = new StringBuilder();
			for(int i = 2;i<params.length;i++)
				idBuilder.append(params[i]+",");
			if(idBuilder.length()>1)
				idBuilder.deleteCharAt(idBuilder.length()-1);
			nameValuePairs.add(new BasicNameValuePair("attachment", idBuilder.toString()));
		}
		conversationId = Integer.parseInt(params[1]);
		body = params[0];
		if(conversationId>0 || conversationId<=-200000000)
			nameValuePairs.add(new BasicNameValuePair("uid", conversationId+""));
		else
			nameValuePairs.add(new BasicNameValuePair("chat_id", -conversationId+""));
		if(coordinates!=null)
		{
			nameValuePairs.add(new BasicNameValuePair("lat", coordinates.split(",")[0]));
			nameValuePairs.add(new BasicNameValuePair("long", coordinates.split(",")[1]));
		}
		if(forwardedMessages!=null)
		{
			nameValuePairs.add(new BasicNameValuePair("forward_messages", forwardedMessages));			
		}
		if(captchaKey!=null) {
            nameValuePairs.add(new BasicNameValuePair("captcha_key", captchaKey));          
		}
        if(captchaSid!=null) {
            nameValuePairs.add(new BasicNameValuePair("captcha_sid", captchaSid));          
        }
	}
	
	   @Override
	    protected void onPostExecute(Message result) {
	        if(handler!=null)
	        {
	            if(errorCode!=ErrorCode.NoError)
	            {
	                if(attempts==0)
	                    handler.OnError(errorCode);
                    callback.OnNetworkError(guid);
	            }
	            else
	            {
	                handler.OnSuccess(result);
	            }       
	        }
	        if(callback!=null && exception!=null)
	            callback.OnSendError(exception);
	        super.onPostExecute(result);
	    }
	    
	    @Override
	    protected void onProgressUpdate(Void... values) {
	        if(handler!=null)
	            handler.OnError(errorCode);
	        super.onProgressUpdate(values);
	    }
	    
	    public String getGuid() {
            return guid;
        }

        public void setGuid(String guid) {
            this.guid = guid;
        }

        public interface SendAsyncCallback<U> extends AsyncCallback<U>
	    {
            public void OnNetworkError(String guid);
	        public void OnSendError(JsonParseException error);
	    }
}
