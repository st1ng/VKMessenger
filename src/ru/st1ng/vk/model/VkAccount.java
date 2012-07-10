package ru.st1ng.vk.model;

import android.graphics.Bitmap;

public class VkAccount extends User {

	
	public String username;
	
	public String token;
		
	public String secret;
	
	@Override
	public String toString() {
		return username;
	}
}
