package ru.st1ng.vk.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.content.Context;
import android.util.Log;


public class NetworkWorker {

	private Context context;
	private final String TAG = "NetworkWorker";
	private NetworkWorker()
	{
	}
	
	private NetworkWorker(Context context)
	{
		this.context = context;
	}
	
	private static String error = "";

	public String getError()
	{
		return error;		
	}
	
	/**
	 * SingletonHolder is loaded on the first execution of
	 * DataProvider.getInstance() or the first access to
	 * SingletonHolder.INSTANCE, not before.
	 */
	private static class SingletonHolder {
		public static NetworkWorker INSTANCE;
	}

	public static NetworkWorker getInstance(Context context) {
		if(SingletonHolder.INSTANCE==null)
			SingletonHolder.INSTANCE = new NetworkWorker(context);
		return SingletonHolder.INSTANCE;
	}
	

	
	public String queryPostString(String URL, String content)
	{
		BufferedReader stream = new BufferedReader(new InputStreamReader(queryPost(URL, content)));
		StringBuilder builder = new StringBuilder();
		String line;
		try {
			while((line=stream.readLine())!=null)
			{
				builder.append(line);
			}
		} catch (IOException e) {
			return "Stream read error";
		}
		return builder.toString();
	}
	
	private InputStream queryPost(String URL, String content)
	{
		return queryPost(URL, content, null);
	}
	
	private InputStream queryPost(String URL, String content, ArrayList<NameValuePair> nameValuePairs) {
		try {
			HttpParams httpParameters = new BasicHttpParams();
			int timeoutConnection = 20000;
			int timeoutSocket = 30000;
			HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
			HttpConnectionParams.setConnectionTimeout(httpParameters,
					timeoutConnection);
			httpParameters.setParameter("http.useragent", "Android mobile");

			HttpClient httpclient = new DefaultHttpClient(httpParameters);
			httpclient.getParams().setParameter("http.useragent", "Android mobile");
			HttpPost httppost = new HttpPost(URL);
			httppost.addHeader("Accept-Encoding", "gzip");
			
			if(nameValuePairs!=null)
				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			if(content!=null)
				httppost.setEntity(new StringEntity(content));
			
			HttpResponse response = httpclient.execute(httppost);
			InputStream instream = response.getEntity().getContent();
			Header contentEncoding = response.getFirstHeader("Content-Encoding");
			if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
			    instream = new GZIPInputStream(instream);
			}
			return instream;
		} catch(SocketTimeoutException ex) {
			error = ex.getMessage();
			return null;
		} catch (Exception e) {
			Log.e(TAG, "Error in http connection " + e.toString());
			error = "Error in connection";
			return null;
		}

	}
	
	private InputStream queryGet(String URL) {
		try {
			HttpParams httpParameters = new BasicHttpParams();
			int timeoutConnection = 20000;
			int timeoutSocket = 30000;
			HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
			HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
			httpParameters.setParameter("http.useragent", "Android mobile");

			HttpClient httpclient = new DefaultHttpClient(httpParameters);
			HttpGet httpget = new HttpGet(URL);
			httpget.addHeader("Accept-Encoding", "gzip");
			httpget.setParams(httpParameters);
			HttpResponse response = httpclient.execute(httpget);
			InputStream instream = response.getEntity().getContent();
			Header contentEncoding = response.getFirstHeader("Content-Encoding");
			if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
			    instream = new GZIPInputStream(instream);
			}
			return instream;
		} catch(SocketTimeoutException ex) {
			error = ex.getMessage();
			return null;
		} catch (Exception e) {
			Log.e(TAG, "Error in http connection " + e.toString());
			error = "Error in connection";
			return null;
		}

	}
	
	/**
	 * Perform getting request
	 * @param contentType: The type of MIME
	 * @param url: The url of get request
	 * @param user: VkAccount if for authorization
	 * @param pass: Password of user for authorization
	 * @return InputStream: The input stream of get request.
	 * @throws IOException
	 */
	private synchronized InputStream performGetRequest(String url) throws IOException{
		
		try {
		HttpParams httpParameters = new BasicHttpParams();
		int timeoutConnection = 20000;
		int timeoutSocket = 30000;
		HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
		HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
		httpParameters.setParameter("http.useragent", "Android mobile");
		HttpClient client = new DefaultHttpClient(httpParameters);
		
		HttpGet httpget = new HttpGet(url);
		httpget.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		httpget.addHeader("Accept-Encoding", "gzip");
		httpParameters.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
		httpget.setParams(httpParameters);
		
		HttpResponse response = client.execute(httpget);
		InputStream instream = response.getEntity().getContent();
		Header contentEncoding = response.getFirstHeader("Content-Encoding");
		if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
		    instream = new GZIPInputStream(instream);
		}
		return instream;
		} catch(SocketTimeoutException ex) {
			error = ex.getMessage();
			return null;
		} catch (Exception e) {
			Log.e(TAG, "Error in http connection " + e.toString());
			error = "Error in connection";
			return null;
		}
	}
		
	/**
	 * Convert input stream to string
	 * @param is Input Stream
	 * @return String
	 * @throws Exception
	 */
	private synchronized String convertStreamToString(InputStream is) throws Exception
	{
		if(is != null)
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(is,"iso-8859-1"),8);
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null)
			{
				sb.append(line + "\n");
			}
			is.close();
			return sb.toString();
		}else {
			Log.v(TAG, "Server response is null");
			return null;
		}
	}
	
	/**
	 * Mapping record type on local to one's on server
	 * @param recordTypeOnLocal The record type on Local
	 * @return record type on server
	 */
	private String mappingRecordType(String recordTypeOnLocal)
	{
		if(recordTypeOnLocal.equals("Record"))
			return "pain_record";
		return null;
	}
	
	

}

