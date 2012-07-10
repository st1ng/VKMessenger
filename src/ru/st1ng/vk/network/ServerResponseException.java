package ru.st1ng.vk.network;

import android.content.Context;

public class ServerResponseException extends Exception {

	private int errCode = -1;
	Context context;
	
	public ServerResponseException(int errorCode,Context context) {
		this.errCode = errorCode;
		this.context = context;
	}
	
	@Override
	public String getMessage() {
		return errString(errCode);
	}
	
	/**
	 * Getting request error string from server response
	 * @param errCode is error string get from server response
	 * @return 
	 */
	private String errString(int errCode)
	{
		return "Hello";
	}
}
