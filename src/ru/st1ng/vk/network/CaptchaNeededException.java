package ru.st1ng.vk.network;

import ru.st1ng.vk.model.Message;

public class CaptchaNeededException extends JsonParseException {

    private static final long serialVersionUID = 1L;

    private Message message;
    private String captcha_sid;
    private String captcha_img;
    public CaptchaNeededException(ErrorCode errorCode, Message message, String captcha_sid, String captcha_img) {
        super(errorCode);
        this.message = message;
        this.captcha_sid = captcha_sid;
        this.captcha_img = captcha_img;
    }
    
    public Message getMsg() {
        return message;
    }
    public String getCaptchaSid() {
        return captcha_sid;
    }
    
    public String getCaptchaImg() {
        return captcha_img;
    }
}
