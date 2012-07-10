package ru.st1ng.vk.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Util { 
 
    private static String convertToHex(byte[] data) { 
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) { 
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do { 
                if ((0 <= halfbyte) && (halfbyte <= 9)) 
                    buf.append((char) ('0' + halfbyte));
                else 
                    buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while(two_halfs++ < 1);
        } 
        return buf.toString();
    } 
 
    public static String MD5(String text)   { 
        MessageDigest md;
        try {
			md = MessageDigest.getInstance("MD5");
	        byte[] md5hash = new byte[32];
	        md.update(text.getBytes());
	        md5hash = md.digest();
	        return convertToHex(md5hash);
		} catch (NoSuchAlgorithmException e) {
		}
        return null;
    } 
} 