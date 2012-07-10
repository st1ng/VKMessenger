package ru.st1ng.vk.model;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.widget.Checkable;

public class Message implements Checkable{
	
	public long mid;
	
	public int uid;
	
	public long date;
	
	public boolean read_state;
	
	public boolean out;
	
	public String title;
	
	public String body;
	
	public int chat_id = 0;
	
	public String chat_active;
	
	public byte users_count;
	
	public int admin_id;
	
	public ArrayList<Attachment> attachments;
	
	public User user;
		
	public ArrayList<User> chat_users;

	public Bitmap dialogBitmap;
	
	public boolean pendingSend = false;
	
	public boolean sent = false;
	
	public boolean networkError = false;
	
	public boolean fromDb = false;
	public String dateString;

	public String timeString;
	
	public boolean forwarded;
	
	public boolean deleted;

	public String guid;
	
	public int getId()
	{
		if(uid<-200000000)
			return uid;
		return chat_id > 0 ? -chat_id : uid;
	}
	
	public List<Message> fwd_messages;
	@Override
	public boolean equals(Object o) {
		if(o.getClass()==Message.class)
			return ((Message)o).mid==this.mid;
		return super.equals(o);
	}
	
	
	private boolean isChecked;

	@Override
	public boolean isChecked() {
		return isChecked;
	}

	@Override
	public void setChecked(boolean checked) {
		isChecked = checked;
	}

	@Override
	public void toggle() {
		isChecked=!isChecked;
	}
}
