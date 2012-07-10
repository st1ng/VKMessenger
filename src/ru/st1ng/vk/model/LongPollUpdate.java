package ru.st1ng.vk.model;

import java.util.ArrayList;

public class LongPollUpdate {

	public String ts;

	public ArrayList<Update> updates;
	
	public class Update
	{
		public int type;
		
		public long id;
		
		public int flags;
		
		public int from_id;
		
		public String title;
		
		public String text;
		
		public long timestamp;
		
		public int from_in_chat;
		
		public ArrayList<Attachment> attachments;
		
		public boolean haveFwd = false;
	}
}
