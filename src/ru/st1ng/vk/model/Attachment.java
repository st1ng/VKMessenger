package ru.st1ng.vk.model;

import android.graphics.Bitmap;

public abstract class Attachment {

	public enum Type {
		Photo(0),Video(1),Wall(2),Audio(3),Doc(4),Geo(5),Forward(6);
		
		private int id;
		private Type(int id)
		{
			this.id = id;
		}
		public int getId()
		{
			return id;
		}
	}
	
	public int id;
	
	public int owner_id;
	
	public abstract Type getType();
	
	public Bitmap bitmap;
	
	public String string_id;
}
