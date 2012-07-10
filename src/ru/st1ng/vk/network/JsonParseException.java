package ru.st1ng.vk.network;

public class JsonParseException extends Exception {

	ErrorCode errCode;
	private String param;
	public JsonParseException(ErrorCode errorCode)
	{
		errCode = errorCode;
	}
	
	public JsonParseException(ErrorCode errorCode, String guid) {
	    errCode = errorCode;
	    this.param = guid;
    }

    public ErrorCode getErrorCode()
	{
		return errCode;
	}

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }
	
}
