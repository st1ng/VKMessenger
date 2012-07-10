package ru.st1ng.vk.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import ru.st1ng.vk.util.WatcherMultipartEntity.OutProgressListener;
import android.util.Log;

public class HttpUtil {
	
	private static final String TAG = "HttpUtil";
	
	public static String getHttps(String url)
	{
		return getHttps(url, null);
	}
	
	public static String getHttps(String url, ArrayList<NameValuePair> nameValuePairs)
	{
		try {
			if(nameValuePairs!=null)
			{
				StringBuilder urlBuilder = new StringBuilder(url);
				urlBuilder.append("?");
				for(NameValuePair pair : nameValuePairs)
				{
					if(pair.getValue()!=null)
					{
						urlBuilder.append(pair.getName()+"="+pair.getValue());
					}
					urlBuilder.append("&");
				}
				url = urlBuilder.toString();
			}
			
			X509HostnameVerifier hostnameVerifier = SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
			
			SchemeRegistry registry = new SchemeRegistry();
		    SSLSocketFactory socketFactory = SSLSocketFactory.getSocketFactory();
		    socketFactory.setHostnameVerifier((X509HostnameVerifier) hostnameVerifier);
		    registry.register(new Scheme("https", socketFactory, 443));

		    
		    HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
		    
			HttpParams httpParameters = new BasicHttpParams();
			int timeoutConnection = 4000;
			int timeoutSocket = 10000;
			HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
			HttpConnectionParams.setConnectionTimeout(httpParameters,
					timeoutConnection);
			httpParameters.setParameter("http.useragent", "Android mobile");

		    SingleClientConnManager mgr = new SingleClientConnManager(httpParameters, registry);
			HttpClient httpclient = new DefaultHttpClient(mgr, httpParameters);
			httpclient.getParams().setParameter("http.useragent",
					"Android mobile");
			HttpGet httpget = new HttpGet(url);
			httpget.addHeader("Accept-Encoding", "gzip");

			
			HttpResponse response = httpclient.execute(httpget);
			InputStream instream = response.getEntity().getContent();
			Header contentEncoding = response
					.getFirstHeader("Content-Encoding");
			if (contentEncoding != null
					&& contentEncoding.getValue().equalsIgnoreCase("gzip")) {
				instream = new GZIPInputStream(instream);
			}
			return HttpUtil.convertStreamToString(instream);

		} catch (SocketTimeoutException ex) {
			//error = ex.getMessage();
			return null;
		} catch (Exception e) {
			Log.e(TAG, "Error in http connection " + e.toString());
		//	error = "Error in connection";
			return null;
		}
	}
	
	public static String getHttpsSig(String url, ArrayList<NameValuePair> nameValuePairs, String secret)
	{
		try {
			if(nameValuePairs!=null)
			{
				StringBuilder urlBuilder = new StringBuilder(url);
				urlBuilder.append("?");
				for(NameValuePair pair : nameValuePairs)
				{
					if(pair.getValue()!=null)
					{
						urlBuilder.append(pair.getName()+"="+URLEncoder.encode(pair.getValue()));
					}
					urlBuilder.append("&");
				}
				url = urlBuilder.toString();
			}
			String secretSid = url.substring(url.lastIndexOf('/')-7,url.length()-1);
			url= url + "sig=" + MD5Util.MD5(secretSid+secret);

			X509HostnameVerifier hostnameVerifier = SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
			
			SchemeRegistry registry = new SchemeRegistry();
		    SSLSocketFactory socketFactory = SSLSocketFactory.getSocketFactory();
		    socketFactory.setHostnameVerifier((X509HostnameVerifier) hostnameVerifier);
		    registry.register(new Scheme("https", socketFactory, 443));

		    
		    HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
		    
			HttpParams httpParameters = new BasicHttpParams();
			int timeoutConnection = 4000;
			int timeoutSocket = 10000;
			HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
			HttpConnectionParams.setConnectionTimeout(httpParameters,
					timeoutConnection);
			httpParameters.setParameter("http.useragent", "Android mobile");

		    SingleClientConnManager mgr = new SingleClientConnManager(httpParameters, registry);
			HttpClient httpclient = new DefaultHttpClient(mgr, httpParameters);
			httpclient.getParams().setParameter("http.useragent",
					"Android mobile");
			HttpGet httpget = new HttpGet(url);
			httpget.addHeader("Accept-Encoding", "gzip");

			
			HttpResponse response = httpclient.execute(httpget);
			InputStream instream = response.getEntity().getContent();
			Header contentEncoding = response
					.getFirstHeader("Content-Encoding");
			if (contentEncoding != null
					&& contentEncoding.getValue().equalsIgnoreCase("gzip")) {
				instream = new GZIPInputStream(instream);
			}
			return HttpUtil.convertStreamToString(instream);

		} catch (SocketTimeoutException ex) {
			//error = ex.getMessage();
			return null;
		} catch (Exception e) {
			Log.e(TAG, "Error in http connection " + e.toString());
		//	error = "Error in connection";
			return null;
		}
	}
	
	public static String getHttpSig(String url, ArrayList<NameValuePair> nameValuePairs, String secret) throws Exception
	{
			if(nameValuePairs!=null)
			{
				StringBuilder urlBuilder = new StringBuilder(url);
				urlBuilder.append("?");
				for(NameValuePair pair : nameValuePairs)
				{
					if(pair.getValue()!=null)
					{
						String value = URLEncoder.encode(pair.getValue());
						value = value.replace("*", "%2A");  
						value = value.replace("~", "%7E");  
						urlBuilder.append(pair.getName()+"="+value);
					}
					urlBuilder.append("&");
				}
				url = urlBuilder.toString();
			}
			String secretSid = url.substring(url.lastIndexOf('/')-7,url.length()-1);
			url= url + "sig=" + MD5Util.MD5(secretSid+secret);
			url = url.replace(" ", "%20");
			HttpParams httpParameters = new BasicHttpParams();
			int timeoutConnection = 6000;
			int timeoutSocket = 10000;
			HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
			HttpConnectionParams.setConnectionTimeout(httpParameters,
					timeoutConnection);
			httpParameters.setParameter("http.useragent", "Android mobile");

			HttpClient httpclient = new DefaultHttpClient(httpParameters);
			httpclient.getParams().setParameter("http.useragent",
					"Android mobile");
			HttpGet httpget = new HttpGet(url);
			httpget.addHeader("Accept-Encoding", "gzip");

			HttpResponse response = httpclient.execute(httpget);
			InputStream instream = response.getEntity().getContent();
			Header contentEncoding = response
					.getFirstHeader("Content-Encoding");
			if (contentEncoding != null
					&& contentEncoding.getValue().equalsIgnoreCase("gzip")) {
				instream = new GZIPInputStream(instream);
			}
			return HttpUtil.convertStreamToString(instream);

	}
	
	public static String getHttp(String url, ArrayList<NameValuePair> nameValuePairs) throws Exception
	{
			if(nameValuePairs!=null)
			{
				StringBuilder urlBuilder = new StringBuilder(url);
				urlBuilder.append("?");
				for(NameValuePair pair : nameValuePairs)
				{
					if(pair.getValue()!=null)
					{
						String value = URLEncoder.encode(pair.getValue());
						value = value.replace("*", "%2A");  
						value = value.replace("~", "%7E");  
						urlBuilder.append(pair.getName()+"="+value);
					}
					urlBuilder.append("&");
				}
				url = urlBuilder.toString();
			}
			HttpParams httpParameters = new BasicHttpParams();
			int timeoutConnection = 6000;
			int timeoutSocket = 10000;
			HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
			HttpConnectionParams.setConnectionTimeout(httpParameters,
					timeoutConnection);
			httpParameters.setParameter("http.useragent", "Android mobile");

			HttpClient httpclient = new DefaultHttpClient(httpParameters);
			httpclient.getParams().setParameter("http.useragent",
					"Android mobile");
			HttpGet httpget = new HttpGet(url);
			httpget.addHeader("Accept-Encoding", "gzip");

			HttpResponse response = httpclient.execute(httpget);
			InputStream instream = response.getEntity().getContent();
			Header contentEncoding = response
					.getFirstHeader("Content-Encoding");
			if (contentEncoding != null
					&& contentEncoding.getValue().equalsIgnoreCase("gzip")) {
				instream = new GZIPInputStream(instream);
			}
			return HttpUtil.convertStreamToString(instream);

	}
	
	public static String getHttpSigNotEncode(String url, ArrayList<NameValuePair> nameValuePairs, String secret) throws Exception
	{
			if(nameValuePairs!=null)
			{
				StringBuilder urlBuilder = new StringBuilder(url);
				urlBuilder.append("?");
				for(NameValuePair pair : nameValuePairs)
				{
					if(pair.getValue()!=null)
					{  
						urlBuilder.append(pair.getName()+"="+pair.getValue());
					}
					urlBuilder.append("&");
				}
				url = urlBuilder.toString();
			}
			String secretSid = url.substring(url.lastIndexOf('/')-7,url.length()-1);
			url= url + "sig=" + MD5Util.MD5(secretSid+secret);
			url = url.replace(" ", "%20");
			HttpParams httpParameters = new BasicHttpParams();
			int timeoutConnection = 6000;
			int timeoutSocket = 10000;
			HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
			HttpConnectionParams.setConnectionTimeout(httpParameters,
					timeoutConnection);
			httpParameters.setParameter("http.useragent", "Android mobile");

			HttpClient httpclient = new DefaultHttpClient(httpParameters);
			httpclient.getParams().setParameter("http.useragent",
					"Android mobile");
			HttpGet httpget = new HttpGet(url);
			httpget.addHeader("Accept-Encoding", "gzip");

			HttpResponse response = httpclient.execute(httpget);
			InputStream instream = response.getEntity().getContent();
			Header contentEncoding = response
					.getFirstHeader("Content-Encoding");
			if (contentEncoding != null
					&& contentEncoding.getValue().equalsIgnoreCase("gzip")) {
				instream = new GZIPInputStream(instream);
			}
			return HttpUtil.convertStreamToString(instream);

	}
	
	private static HttpClient downloadingClient;
	public static String downloadUrlToFile(String url, String outPath) throws Exception {
		OutputStream out = null;
		InputStream in = null;
		String result = null;
			URL urladdr = new URL(url);
			File outFile = new File(outPath, getFileNameFromPath(urladdr.getFile()));
			File parent = outFile.getParentFile();
			HttpGet httpGet = new HttpGet(url);
			if(downloadingClient==null)
			{
				HttpParams httpParameters = new BasicHttpParams();
				int timeoutConnection = 4000;
				int timeoutSocket = 10000;
				HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
				HttpConnectionParams.setConnectionTimeout(httpParameters,
						timeoutConnection);
				httpParameters.setParameter("http.useragent", "Android mobile");
				SchemeRegistry registry = new SchemeRegistry();
				registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
				ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(httpParameters, registry);
				downloadingClient = new DefaultHttpClient(cm,httpParameters);
			}
			HttpResponse response = downloadingClient.execute(httpGet);
			InputStream instream = response.getEntity().getContent();
			
			if(!parent.exists())
			{
				parent.mkdirs();
				parent.mkdir();
			}
			outFile.createNewFile();
			
			out = new BufferedOutputStream(new FileOutputStream(outFile));
			in = new BufferedInputStream(instream,512);

			byte[] buffer = new byte[512];
			int numRead;
			while ((numRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, numRead);
			}
			result = outFile.getAbsolutePath();
			try {
				if (in != null) {
					in.close();
				}
				if (out != null) {
					out.close();
				}
			} catch (IOException ioe) {
		}
		return result;
	}
	public static InputStream getInputStream(String url) throws Exception {
			HttpGet httpGet = new HttpGet(url);
			if(downloadingClient==null)
			{
				HttpParams httpParameters = new BasicHttpParams();
				int timeoutConnection = 4000;
				int timeoutSocket = 10000;
				HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
				HttpConnectionParams.setConnectionTimeout(httpParameters,
						timeoutConnection);
				httpParameters.setParameter("http.useragent", "Android mobile");
				SchemeRegistry registry = new SchemeRegistry();
				registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
				ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(httpParameters, registry);
				downloadingClient = new DefaultHttpClient(cm,httpParameters);
			}
			HttpResponse response = downloadingClient.execute(httpGet);
			return response.getEntity().getContent();

	}
	
	public static String downloadUrlToFile(String url, String outPath, String outFileName) throws Exception {
		OutputStream out = null;
		InputStream in = null;
		String result = null;
			URL urladdr = new URL(url);
			File outFile = new File(outPath, outFileName);
			File parent = outFile.getParentFile();
			HttpGet httpGet = new HttpGet(url);
			if(downloadingClient==null)
			{
				HttpParams httpParameters = new BasicHttpParams();
				int timeoutConnection = 4000;
				int timeoutSocket = 10000;
				HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
				HttpConnectionParams.setConnectionTimeout(httpParameters,
						timeoutConnection);
				httpParameters.setParameter("http.useragent", "Android mobile");
				SchemeRegistry registry = new SchemeRegistry();
				registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
				ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(httpParameters, registry);
				downloadingClient = new DefaultHttpClient(cm,httpParameters);
			}
			HttpResponse response = downloadingClient.execute(httpGet);
			InputStream instream = response.getEntity().getContent();
			
			if(!parent.exists())
			{
				parent.mkdirs();
				parent.mkdir();
			}
			outFile.createNewFile();
			
			out = new BufferedOutputStream(new FileOutputStream(outFile));
			in = new BufferedInputStream(instream,512);

			byte[] buffer = new byte[512];
			int numRead;
			while ((numRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, numRead);
			}
			result = outFile.getAbsolutePath();
			try {
				if (in != null) {
					in.close();
				}
				if (out != null) {
					out.close();
				}
			} catch (IOException ioe) {
		}
		return result;
	}
	
	private static String getFileNameFromPath(String url)
	{
		return url.substring(url.lastIndexOf('/')+1, url.length());
	}

	public static String getHttpLongPoll(String url, ArrayList<NameValuePair> nameValuePairs)
	{
		try {
			if(nameValuePairs!=null)
			{
				StringBuilder urlBuilder = new StringBuilder(url);
				urlBuilder.append("?");
				for(NameValuePair pair : nameValuePairs)
				{
					if(pair.getValue()!=null)
					{
						urlBuilder.append(pair.getName()+"="+pair.getValue());
					}
					urlBuilder.append("&");
				}
				url = urlBuilder.toString();
			}
			
		    
			HttpParams httpParameters = new BasicHttpParams();
			int timeoutConnection = 100000;
			int timeoutSocket = 100000;
			HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
			HttpConnectionParams.setConnectionTimeout(httpParameters,
					timeoutConnection);
			httpParameters.setParameter("http.useragent", "Android mobile");

			HttpClient httpclient = new DefaultHttpClient(httpParameters);
			httpclient.getParams().setParameter("http.useragent",
					"Android mobile");
			HttpGet httpget = new HttpGet(url);
			httpget.addHeader("Accept-Encoding", "gzip");

			
			HttpResponse response = httpclient.execute(httpget);
			InputStream instream = response.getEntity().getContent();
			Header contentEncoding = response
					.getFirstHeader("Content-Encoding");
			if (contentEncoding != null
					&& contentEncoding.getValue().equalsIgnoreCase("gzip")) {
				instream = new GZIPInputStream(instream);
			}
			return HttpUtil.convertStreamToString(instream);

		} catch (SocketTimeoutException ex) {
			//error = ex.getMessage();
			return null;
		} catch (Exception e) {
			Log.e(TAG, "Error in http connection " + e.toString());
		//	error = "Error in connection";
			return null;
		}
	}
	
	public static String uploadFile(String url, String filePath, OutProgressListener listener) throws Exception
	{
		MultipartEntity entity = new WatcherMultipartEntity(listener);
		entity.addPart("photo", new FileBody(new File(filePath)));
	  //  fileEntity.setChunked(true);

	    HttpParams httpParameters = new BasicHttpParams();
		int timeoutConnection = 100000;
		int timeoutSocket = 100000;
		HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
		HttpConnectionParams.setConnectionTimeout(httpParameters,
				timeoutConnection);
//		httpParameters.setParameter("http.useragent", "Android mobile");
		
		HttpClient httpclient = new DefaultHttpClient(httpParameters);
//		httpclient.getParams().setParameter("http.useragent",
//				"Android mobile");
		HttpPost httppost = new HttpPost(url);
		httppost.setEntity(entity);
//		httppost.addHeader("Content-Length", "" + new File(filePath).length());
		HttpResponse response = httpclient.execute(httppost);
		InputStream instream = response.getEntity().getContent();
		return HttpUtil.convertStreamToString(instream);

	}
	
	public static String postHttps(String url)
	{
		return postHttps(url, null, null);
	}
	
	public static String postHttps(String url, ArrayList<NameValuePair> nameValuePairs, String content)
	{
		try {
			
			X509HostnameVerifier hostnameVerifier = SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
			
			SchemeRegistry registry = new SchemeRegistry();
		    SSLSocketFactory socketFactory = SSLSocketFactory.getSocketFactory();
		    socketFactory.setHostnameVerifier((X509HostnameVerifier) hostnameVerifier);
		    registry.register(new Scheme("https", socketFactory, 443));

		    
		    HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
		    
			HttpParams httpParameters = new BasicHttpParams();
			int timeoutConnection = 100000;
			int timeoutSocket = 100000;
			HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
			HttpConnectionParams.setConnectionTimeout(httpParameters,
					timeoutConnection);
			httpParameters.setParameter("http.useragent", "Android mobile");

		    SingleClientConnManager mgr = new SingleClientConnManager(httpParameters, registry);
			HttpClient httpclient = new DefaultHttpClient(mgr, httpParameters);
			httpclient.getParams().setParameter("http.useragent",
					"Android mobile");
			HttpPost httppost = new HttpPost(url);
			httppost.addHeader("Accept-Encoding", "gzip");

			if(nameValuePairs!=null)
				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			if(content!=null)
				httppost.setEntity(new StringEntity(content));

			HttpResponse response = httpclient.execute(httppost);
			InputStream instream = response.getEntity().getContent();
			Header contentEncoding = response
					.getFirstHeader("Content-Encoding");
			if (contentEncoding != null
					&& contentEncoding.getValue().equalsIgnoreCase("gzip")) {
				instream = new GZIPInputStream(instream);
			}
			return HttpUtil.convertStreamToString(instream);
		} catch (SocketTimeoutException ex) {
			//error = ex.getMessage();
			return null;
		} catch (Exception e) {
			Log.e(TAG, "Error in http connection " + e.toString());
		//	error = "Error in connection";
			return null;
		}
	}
	
	
	/**
	 * Convert input stream to string
	 * @param is Input Stream
	 * @return String
	 * @throws Exception
	 */
	public static String convertStreamToString(InputStream is) throws Exception
	{
		if(is==null)
			return null;
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null)
		{
			sb.append(line + "\n");
		}
		is.close();
		return sb.toString();
	}
	
}

