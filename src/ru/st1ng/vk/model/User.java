package ru.st1ng.vk.model;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

public class User{

	
	//DATABASE FIELDS START
	public int uid;
	
	public String first_name;

	public String last_name;
	
	public String nick_name;
	
	public String short_name;
	
	public String photo;

	public String photo_medium;

	public int sex = 0;

	public long last_seen;

	public int hintpos = -1;
	
	
	//DATABASE FIELDS END
	
	public Bitmap photo_bitmap;
	
	public Drawable photo_drawable;

	public Bitmap photo_bitmap_small;
	
	public boolean has_mobile = true;
	
	public boolean online;
	
	public boolean writing;
	
	public String phones;

	public boolean request;
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof User)
			return ((User)o).uid==this.uid;
		return super.equals(o);
	}
}
