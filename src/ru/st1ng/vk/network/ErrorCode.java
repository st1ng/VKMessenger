package ru.st1ng.vk.network;

import ru.st1ng.vk.R;

public enum ErrorCode {
	NoError(-1), 
	NetworkUnavailable(R.string.network_unavailable), 
	WrongNameOrPass(R.string.wrong_user_pass),
	AlreadyRegistered(R.string.already_registered_1004, 1004),
	CannotAddToFriendsDueToPrivate(R.string.e176_cannot_add_user, 176),
	PhoneIncorrect(R.string.e100_phone_incorrect,100),
	AccessDenied(R.string.e15_access_denied, 15),
	FloodControl(R.string.e9_floot_control, 9),
	NotFoundInApplication(R.string.unknown_error),
	ParsingError(-3),
	LongPollKeyNeedToBeUpdated(-4),
	CaptchaNeeded(-5), UserNotLoggedIn(-6), MessagesSuccessfullyDeleted(-7);
	
	private int string_id;
	private int server_id;
    ErrorCode(int string_id) { this.string_id = string_id; }
    ErrorCode(int string_id,int server_id) { this.string_id = string_id; this.server_id = server_id; }
    public int getStringResource() { return string_id>0 ? string_id : R.string.unknown_error; }    
    
    
    public static ErrorCode getByServerId(int serverId)
    {
    	for(ErrorCode code : values())
    	{
    		if(code.server_id==serverId)
    			return code;
    	}
    	return NotFoundInApplication;
    }
}
