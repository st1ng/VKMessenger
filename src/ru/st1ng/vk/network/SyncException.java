package ru.st1ng.vk.network;

public class SyncException extends Exception {

	private int errCode = -1;
	public SyncException(int errorCode) {
		this.errCode = errorCode;
	}

	public SyncException(String errorCode) {
		this.errCode = Integer.valueOf(errorCode);
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
	private String errString(String errCode)
	{
		return errString(Integer.valueOf(errCode));
	}
	
	/**
	 * Getting request error string from server response
	 * @param errCode is error string get from server response
	 * @return 
	 */
	private String errString(int errCode)
	{
		String strError = "";
		switch(errCode) {
			case 101:
				strError = "General Error!";
				break;
			case 102:
				strError = "No Post data sent!";
				break;
			case 103:
				strError = "The specified JSON string is not valid!";
				break;
			case 104:
				strError = "No API methods was specified!";
				break;
			case 105:
				strError = "The specified method %method is not a valid API method!";
				break;
			case 130:
				strError = "Username not specified!";
				break;
			case 131:
				strError = "VkAccount ID not specified";
				break;
			case 132:
				strError = "The specified username %username is invalid!";
				break;
			case 133:
				strError = "The specified user ID %user_id is invalid!";
				break;
			case 134:
				strError = "Password not specified!";
				break;
			case 135:
				strError = "The Specified password is incorrect!";
				break;
			default:
				strError = "Unspecified error";
		}
		return strError;
	}
}