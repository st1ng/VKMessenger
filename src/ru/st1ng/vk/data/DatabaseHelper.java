package ru.st1ng.vk.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ru.st1ng.vk.model.ContactName;
import ru.st1ng.vk.model.Message;
import ru.st1ng.vk.model.User;
import ru.st1ng.vk.model.VkAccount;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper{
	private Context context;
	private DBOpenHelper openHelper;
	
	private static final String TABLE_ACCOUNTS = "accounts";
	private static final String TABLE_USERS = "users";
	private static final String TABLE_CONTACTS = "contacts";
	private static final String TABLE_DIALOGS = "dialogs";
	private static final String TABLE_MESSAGES = "messages";
	private static final String TABLE_ATTACHMENTS = "attachments";
	
	public DatabaseHelper(Context context) {
		this.context = context;
		openHelper = new DBOpenHelper(context);
	}

	public synchronized ArrayList<VkAccount> getAccounts()
	{
		ArrayList<VkAccount> result = new ArrayList<VkAccount>();
		SQLiteDatabase db = openHelper.openWrite();
		Cursor cur = db.query(TABLE_ACCOUNTS, null,null,null,null,null,"timestamp desc");
		if (cur.moveToFirst()){
			   do{
			      VkAccount user = new VkAccount();
			      user.uid = cur.getInt(0);
			      user.username = cur.getString(1);
			      user.token = cur.getString(2);
			      user.secret = cur.getString(3);
			      user.photo = cur.getString(4);
			      user.first_name = cur.getString(5);
			      user.last_name = cur.getString(6);
			      result.add(user);
			   } while(cur.moveToNext());
		}
		cur.close();
		db.close();
		return result;
	}
	
	public synchronized boolean addOrReplaceUser(VkAccount user)
	{
		SQLiteDatabase db = openHelper.openWrite();
		ContentValues values = new ContentValues();
		values.put("id", user.uid);
		values.put("username", user.username);
		values.put("token", user.token);
		values.put("secret", user.secret);
		values.put("photo",user.photo);
		values.put("first_name", user.first_name);
		values.put("last_name", user.last_name);
		values.put("timestamp", System.currentTimeMillis());
		long result = db.replace(TABLE_ACCOUNTS, null, values);
		db.close();
		return result>0;
	}
	

	public synchronized void logOff()
	{
		SQLiteDatabase db = openHelper.open();
		ContentValues values = new ContentValues();
		values.put("token", "");
		db.update(TABLE_ACCOUNTS, values, null, null);
		db.close();
	}
	
	public synchronized ArrayList<User> getUsers(int accid)
	{
		ArrayList<User> result = new ArrayList<User>();
		SQLiteDatabase db = openHelper.open();
		Cursor cur = db.query(TABLE_USERS, null,"accid=?",new String[] {accid+""},null,null,null);
		if (cur.moveToFirst()){
			   do{
			      User user = new User();
			      user.uid = cur.getInt(0);
			      user.first_name = cur.getString(2);
			      user.last_name = cur.getString(3);
			      user.nick_name = cur.getString(4);
			      user.short_name = cur.getString(5);
			      user.photo = cur.getString(6);
			      user.sex = cur.getInt(7);
			      user.hintpos = cur.getInt(8);
			      result.add(user);
			   } while(cur.moveToNext());
		}
		cur.close();
		db.close();
		return result;
	}
	
	public synchronized HashMap<String, ContactName> getContacts(int accid)
	{
		HashMap<String, ContactName> result = new HashMap<String, ContactName>();
		SQLiteDatabase db = openHelper.open();
		Cursor cur = db.query(TABLE_CONTACTS, null,"accid=?",new String[] {accid+""},null,null,null);
		if (cur.moveToFirst()){
			   do{
			      ContactName user = new ContactName();
			      user.contactid = cur.getInt(0);
			      user.contact_name = cur.getString(2);
			      user.first_name = cur.getString(3);
			      user.last_name = cur.getString(4);
			      user.nick_name = cur.getString(5);
			      user.short_name = cur.getString(6);
			      user.photo = cur.getString(7);
			      user.sex = cur.getInt(8);
			      user.phones = cur.getString(9);
			      user.uid = cur.getInt(10);
			      user.photoid = cur.getLong(12);
/*			      String users = cur.getString(10);
			      if(users!=null && users.length()>0)
			      {
			    	  
			    	  user.users = new ArrayList<User>();
			    	  String[] usersList = users.substring(1, users.length()-1).split(",");
			    	  for(int i = 0;i<usersList.length;i++)
			    	  {
			    		  User us = new User();
			    		  us.uid = Integer.parseInt(usersList[i]);
				    	  user.users.add(us);
			    	  }
			      }*/
			      result.put(user.phones,user);
			   } while(cur.moveToNext());
		}
		cur.close();
		db.close();
		return result;
	}
	
	public synchronized ArrayList<Message> getDialogs(int accid,int limit)
	{
		ArrayList<Message> result = new ArrayList<Message>();
		SQLiteDatabase db = openHelper.open();
		Cursor cur = db.query(TABLE_DIALOGS, null,"accid=?",new String[] {accid+""},null,null,"date desc",limit+"");
		if (cur.moveToFirst()){
			   do{
				  Message message = new Message();
			      message.mid = cur.getInt(0);
			      message.uid = cur.getInt(1);
			      message.date = cur.getLong(3);
			      message.read_state = cur.getInt(4)>0;
			      message.out = cur.getInt(5)>0;
			      message.title = cur.getString(6);
			      message.body = cur.getString(7);
			      message.chat_id = cur.getInt(8);
			      message.chat_active = cur.getString(9);
			      message.admin_id = cur.getInt(10);
			      result.add(message);
			   } while(cur.moveToNext());
		}
		cur.close();
		db.close();
		return result;
	}
	
	public synchronized ArrayList<Message> getMessages(int accid,int convId,int limit)
	{
		ArrayList<Message> result = new ArrayList<Message>();
		SQLiteDatabase db = openHelper.open();
		String crit = "accid=? and " + (convId>0 ? "uid=?" : "chat_id=?");
		Cursor cur = db.query(TABLE_MESSAGES, null,crit,new String[] {accid+"",""+(convId>0 ? convId : -convId)},null,null,null,limit+"");
		if (cur.moveToFirst()){
			   do{
				  Message message = new Message();
			      message.mid = cur.getInt(0);
			      message.uid = cur.getInt(1);
			      message.date = cur.getLong(3);
			      message.read_state = cur.getInt(4)>0;
			      message.out = cur.getInt(5)>0;
			      message.title = cur.getString(6);
			      message.body = cur.getString(7);
			      message.chat_id = cur.getInt(8);
			      message.chat_active = cur.getString(9);
			      message.admin_id = cur.getInt(10);
			      message.fromDb = true;
			      result.add(message);
			   } while(cur.moveToNext());
		}
		cur.close();
		db.close();
		return result;
	}
	
	public synchronized void updateUsers(List<User> users, int accid)
	{
		SQLiteDatabase db = openHelper.openWrite();
		db.beginTransaction();
		try
		{
			for(User user : users)
			{ 
				ContentValues values = new ContentValues();
				values.put("uid", user.uid);
				values.put("accid", accid);
				values.put("first_name", user.first_name);
				values.put("last_name", user.last_name);
				values.put("nick_name", user.nick_name);
				values.put("short_name", user.short_name);
				values.put("photo", user.photo);
				values.put("sex", user.sex);
				if(user.hintpos>=0)
					values.put("hint", user.hintpos);
				db.replace(TABLE_USERS, null, values);
			}
			db.setTransactionSuccessful();
		} catch(Exception e) {
		} finally {
			db.endTransaction();
		}
		
		db.close();
	}
	
	public synchronized void updateContacts(List<ContactName> users, int accid)
	{
		SQLiteDatabase db = openHelper.openWrite();
		db.beginTransaction();
		try
		{
			for(ContactName u : users)
			{ 
				ContactName user  = (ContactName) u;
				ContentValues values = new ContentValues();
				values.put("contactid", user.contactid);
				values.put("accid", accid);
				values.put("first_name", user.first_name);
				values.put("last_name", user.last_name);
				values.put("contact_name", user.contact_name);
				values.put("short_name", user.short_name);
				values.put("photo", user.photo);
				values.put("phone", user.phones);
				values.put("sex", user.sex);
				values.put("photoid", user.photoid);
				if(user.uid>=0)
					values.put("userid",user.uid);
				if(user.users!=null)
					values.put("users_id", user.users.toString());
				db.replace(TABLE_CONTACTS, null, values);
			}
			db.setTransactionSuccessful();
		} catch(Exception e) {
		} finally {
			db.endTransaction();
		}
		 
		db.close();
	}
	
	public synchronized void updateDialogs(List<Message> messages, int accid)
	{
		SQLiteDatabase db = openHelper.openWrite();
		db.beginTransaction();
		try
		{
			for(Message message : messages)
			{ 
				ContentValues values = new ContentValues();
				values.put("mid", message.mid);
				values.put("uid", message.uid);
				values.put("date",message.date);
				values.put("accid", accid);
				values.put("title", message.title);
				values.put("body", message.body);
				values.put("read_state", message.read_state ? 1 : 0);
				values.put("out", message.out ? 1 : 0);
				values.put("chat_id",message.chat_id);
				if(message.chat_active!=null)
					values.put("chat_active",message.chat_active);
				values.put("admin_id", message.admin_id);
				db.replace(TABLE_DIALOGS, null, values);
			}
			db.setTransactionSuccessful();
		} catch(Exception e) {
		} finally {
			db.endTransaction();
		}
		
		db.close();
	}
	
	public synchronized void updateMessages(List<Message> messages, int accid, int convId)
	{
		SQLiteDatabase db = openHelper.openWrite();
		db.beginTransaction();
		try
		{ 
			for(Message message : messages)
			{ 
				ContentValues values = new ContentValues();
				values.put("mid", message.mid);
				values.put("uid", convId > 0 ? convId : message.uid);
				values.put("date",message.date);
				values.put("accid", accid);
				values.put("title", message.title);
				values.put("body", message.body);
				values.put("read_state", message.read_state ? 1 : 0);
				values.put("out", message.out ? 1 : 0);
				values.put("chat_id", convId < 0 ? convId : message.chat_id);
				if(message.chat_active!=null)
					values.put("chat_active",message.chat_active);
				values.put("admin_id", message.admin_id);
				db.replace(TABLE_MESSAGES, null, values);
			}
			db.setTransactionSuccessful();
		} catch(Exception e) {
		} finally {
			db.endTransaction();
		}
		
		db.close();
	}
	
	public void deleteMessagesById(List<Integer> ids)
	{
		SQLiteDatabase db = openHelper.openWrite();
		db.beginTransaction();
		try
		{ 
			for(Integer id : ids)
			{ 
				db.delete(TABLE_MESSAGES, "mid = ?", new String[] { String.valueOf(id) });
				db.delete(TABLE_DIALOGS, "mid = ?", new String[] { String.valueOf(id) });
			}
			db.setTransactionSuccessful();
		} catch(Exception e) {
		} finally {
			db.endTransaction();
		}
		
		db.close();
	}

	   
    public void deleteUserById(Integer id, boolean deleteAllMessages)
    {
        SQLiteDatabase db = openHelper.openWrite();
        db.beginTransaction();
        try
        { 
                    if(deleteAllMessages)
                        db.delete(TABLE_MESSAGES, "uid = ?", new String[] { String.valueOf(id) });
                    db.delete(TABLE_USERS, "uid = ?", new String[] { String.valueOf(id) });
            db.setTransactionSuccessful();
        } catch(Exception e) {
        } finally {
            db.endTransaction();
        }
        
        db.close();
    }
    
	public void deleteDialogsById(Integer id, boolean deleteAllMessages)
	{
		SQLiteDatabase db = openHelper.openWrite();
		db.beginTransaction();
		try
		{ 
				if(id>0) {					
					if(deleteAllMessages)
						db.delete(TABLE_MESSAGES, "uid = ?", new String[] { String.valueOf(id) });
					db.delete(TABLE_DIALOGS, "uid = ?", new String[] { String.valueOf(id) });
				} else {
					if(deleteAllMessages)
						db.delete(TABLE_MESSAGES, "chat_id = ?", new String[] { String.valueOf(-id) });
					db.delete(TABLE_DIALOGS, "chat_id = ?", new String[] { String.valueOf(-id) });					
				}
			db.setTransactionSuccessful();
		} catch(Exception e) {
		} finally {
			db.endTransaction();
		}
		
		db.close();
	}
	private class DBOpenHelper extends SQLiteOpenHelper
	{
		private static final String DBNAME = "v.db";
		private static final int version = 7;

		private static final String CREATE_ACCOUNTS = "CREATE TABLE " + TABLE_ACCOUNTS + "('id' INTEGER primary key, 'username' TEXT, 'token' TEXT,'secret' TEXT, 'photo' TEXT, 'first_name' TEXT,'last_name' TEXT, 'timestamp' LONG);";
		private static final String CREATE_USERS = "CREATE TABLE " + TABLE_USERS + "('uid' INTEGER primary key, 'accid' INTEGER, 'first_name' TEXT, 'last_name' TEXT, 'nick_name' TEXT, 'short_name' TEXT,'photo' TEXT, 'sex' INTEGER, 'hint' INTEGER default -1);";
		private static final String CREATE_DIALOGS = "CREATE TABLE " + TABLE_DIALOGS + "('mid' INTEGER primary key desc,'uid' INTEGER, 'accid' INTEGER, 'date' LONG, 'read_state' INTEGER,'out' INTEGER, 'title' TEXT, 'body' TEXT,'chat_id' INTEGER, 'chat_active' TEXT, 'admin_id' INTEGER, UNIQUE('uid','chat_id') ON CONFLICT REPLACE);";
		private static final String CREATE_ATTACHMENTS = "CREATE TABLE " + TABLE_ATTACHMENTS + "('id' INTEGER primary key desc,'uid' INTEGER, 'accid' INTEGER, 'date' LONG, 'read_state' INTEGER,'out' INTEGER, 'title' TEXT, 'body' TEXT,'chat_id' INTEGER, 'chat_active' TEXT, 'admin_id' INTEGER, UNIQUE('uid','chat_id') ON CONFLICT REPLACE);";
		private static final String CREATE_CONTACTS = "CREATE TABLE " + TABLE_CONTACTS + "('contactid' INTEGER primary key,  'accid' INTEGER,'contact_name' TEXT, 'first_name' TEXT, 'last_name' TEXT, 'nick_name' TEXT, 'short_name' TEXT,'photo' TEXT, 'sex' INTEGER, 'phone' TEXT, 'userid' INTEGER,'users_id' TEXT,'photoid' LONG);";
		private static final String CREATE_MESSAGES = "CREATE TABLE " + TABLE_MESSAGES + "('mid' INTEGER primary key desc,'uid' INTEGER, 'accid' INTEGER, 'date' LONG, 'read_state' INTEGER,'out' INTEGER, 'title' TEXT, 'body' TEXT,'chat_id' INTEGER, 'chat_active' TEXT, 'admin_id' INTEGER);";

		private static final String UPDATE_2 = "ALTER TABLE " + TABLE_USERS + " ADD COLUMN 'hint' INTEGER";
		private static final String UPDATE_3 = CREATE_CONTACTS; //Version 0.8
		private static final String UPDATE_4 = CREATE_MESSAGES; //Version 0.9
		private static final String UPDATE_4_1 = CREATE_ATTACHMENTS; //Version 0.9
		private static final String UPDATE_5 = "DELETE FROM " + TABLE_MESSAGES;
        private static final String UPDATE_7 = "ALTER TABLE " + TABLE_CONTACTS + " ADD COLUMN 'photoid' LONG";
        private static final String UPDATE_7_1 = "DELETE FROM " + TABLE_CONTACTS;

        public DBOpenHelper(Context context) {
			super(context, DBNAME, null, version);
		}

		@Override
		public synchronized void onCreate(SQLiteDatabase db) {
			db.execSQL(CREATE_ACCOUNTS);
			db.execSQL(CREATE_USERS);
			db.execSQL(CREATE_DIALOGS);
			db.execSQL(UPDATE_3);
			db.execSQL(UPDATE_4);
			db.execSQL(UPDATE_4_1);
		}
	
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if(newVersion==2)
				db.execSQL(UPDATE_2);
			 if(newVersion==3)
			 {
				db.execSQL(UPDATE_3);
			 }
			 if(newVersion>=4 && oldVersion<4)
			 {
				 if(oldVersion==2)
					db.execSQL(UPDATE_3);
				 db.execSQL(UPDATE_4);
				 db.execSQL(UPDATE_4_1);
			 }
			 if(newVersion>=5 && oldVersion<5)
			 {
			     db.execSQL(UPDATE_5);
			 }
			 if(newVersion>=7 && oldVersion<7) {
			     db.execSQL(UPDATE_7);
			     db.execSQL(UPDATE_7_1);
			 }
		}
		
		private SQLiteDatabase open()
		{
			return getReadableDatabase();
		}
		
		private SQLiteDatabase openWrite()
		{
			return getWritableDatabase();
		}
	}


}
