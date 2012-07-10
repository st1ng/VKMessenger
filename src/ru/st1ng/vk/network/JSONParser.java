package ru.st1ng.vk.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.st1ng.vk.R;
import ru.st1ng.vk.VKApplication;
import ru.st1ng.vk.model.Attachment;
import ru.st1ng.vk.model.AudioAttach;
import ru.st1ng.vk.model.DocAttach;
import ru.st1ng.vk.model.GeoAttach;
import ru.st1ng.vk.model.LongPollServer;
import ru.st1ng.vk.model.LongPollUpdate;
import ru.st1ng.vk.model.LongPollUpdate.Update;
import ru.st1ng.vk.model.Message;
import ru.st1ng.vk.model.PhotoAttach;
import ru.st1ng.vk.model.ServerUploadFile;
import ru.st1ng.vk.model.User;
import ru.st1ng.vk.model.VideoAttach;
import ru.st1ng.vk.model.VkAccount;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.location.Geocoder;
import android.provider.MediaStore.Video;
import android.util.Log;

/**
 * @author st1ng
 * Class for parsing all JSON responses
 */
public class JSONParser {
	
	private final String TAG = "JSONParser";

	public static VkAccount parseAuthResponse(String jsonString) throws JsonParseException
	{
		try {
			JSONObject json = new JSONObject(jsonString);
			if(json.has("error"))
			{
				if(json.getString("error").equals("need_captcha"))
					throw new JsonParseException(ErrorCode.CaptchaNeeded);
				throw new JsonParseException(ErrorCode.WrongNameOrPass);
			}
			VkAccount result = new VkAccount();
			result.token = json.getString("access_token");
			result.uid = json.getInt("user_id");
			result.secret = json.getString("secret");
			return result;
		} catch (JSONException e) {
			throw new JsonParseException(ErrorCode.ParsingError);
		}	
	}
	
	
	/**
	 * @param jsonString
	 * @return Signup SID string
	 * @throws JsonParseException - if any error was happen
	 */
	public static String parseSignupResponse(String jsonString) throws JsonParseException
	{
		if(jsonString==null)
			return null;
		try {
			JSONObject json = new JSONObject(jsonString);
			if(json.has("error"))
				throw new JsonParseException(ErrorCode.getByServerId(json.getJSONObject("error").getInt("error_code")));
			json = json.getJSONObject("response");
			return json.getString("sid");
		} catch (Exception e) {
			throw new JsonParseException(ErrorCode.ParsingError);
		}		
	}

       public static Boolean parseCheckPhoneResponse(String jsonString) throws JsonParseException {
            try {
                JSONObject json = new JSONObject(jsonString);
                if(json.has("error"))
                    throw new JsonParseException(ErrorCode.getByServerId(json.getJSONObject("error").getInt("error_code")));
                return json.getInt("response")>0;
            } catch (JSONException e) {
                throw new JsonParseException(ErrorCode.ParsingError);
            }   
        }

	/**
	 * @param jsonString
	 * @return Signup SID string
	 * @throws JsonParseException - if any error was happen
	 */
	public static Message parseGetChatResponse(String jsonString) throws JsonParseException
	{
		if(jsonString==null)
			return null;
		try {
			JSONObject json = new JSONObject(jsonString);
			if(json.has("error"))
				throw new JsonParseException(ErrorCode.getByServerId(json.getJSONObject("error").getInt("error_code")));
			json = json.getJSONObject("response");
			Message result = new Message();
			result.title = json.getString("title");
			if(json.has("admin_id"))
				result.admin_id = json.getInt("admin_id");
			JSONArray users = json.getJSONArray("users");
			StringBuilder usersStr = new StringBuilder();
			for(int i = 0;i<users.length();i++)
			{
				usersStr.append(users.get(i) +",");
			}
			if(usersStr.length()>0)
				usersStr.deleteCharAt(usersStr.length()-1);
			result.chat_active = usersStr.toString();
			return result;
		} catch (Exception e) {
			throw new JsonParseException(ErrorCode.ParsingError);
		}		
	}
	
	public static List<Message> parseGetMessagesResponse(String jsonString) throws JsonParseException
	{
		return parseGetMessagesResponse(jsonString, 0,false,false);
	}
	public static List<Message> parseGetMessagesResponse(String jsonString, int ownId) throws JsonParseException
	{
		return parseGetMessagesResponse(jsonString, ownId,false,false);
	}	
	/**
	 * @param jsonString
	 * @return Signup SID string
	 * @throws JsonParseException - if any error was happen
	 */
	public static List<Message> parseGetMessagesResponse(String jsonString, int ownId,boolean fwd_message,boolean array) throws JsonParseException
	{
		try {
			ArrayList<Message> result = new ArrayList<Message>();
			JSONArray jsonMessages;
			if(!fwd_message && !array)
			{
				JSONObject json = new JSONObject(jsonString);
				if(json.has("error"))
					throw new JsonParseException(ErrorCode.getByServerId(json.getJSONObject("error").getInt("error_code")));
				jsonMessages = json.getJSONArray("response");
			}
			else
			{
				jsonMessages = new JSONArray(jsonString);
			}
			
			for(int i = jsonMessages.length()-1; i> (fwd_message ? -1 : 0);i--)
			{
				JSONObject jsonMessage = jsonMessages.getJSONObject(i);
				Message message = new Message();
				if(!fwd_message)
				{
					message.mid = jsonMessage.getLong("mid");
					message.read_state = jsonMessage.getInt("read_state")>0;
					if(jsonMessage.has("out"))
						message.out = jsonMessage.getInt("out")>0;
					else
						message.out = (message.uid == ownId);						
				}
				else
				{
					message.forwarded = true;
				}
				message.uid = jsonMessage.getInt("uid");
				message.date = jsonMessage.getLong("date");
				message.body = jsonMessage.getString("body").replace("<br>", "\n");
				if(jsonMessage.has("title"))
					message.title = jsonMessage.getString("title");
				if(jsonMessage.has("chat_id"))
				{
					message.chat_id = jsonMessage.getInt("chat_id");
					if(jsonMessage.has("chat_active"))
						message.chat_active = jsonMessage.getString("chat_active");
					if(jsonMessage.has("admin_id"))
						message.admin_id = jsonMessage.getInt("admin_id");
				}
				if(jsonMessage.has("geo"))
				{
					if(message.attachments==null)
						message.attachments = new ArrayList<Attachment>();
					
					GeoAttach attach = new GeoAttach();
					String coords = jsonMessage.getJSONObject("geo").getString("coordinates");
					String[] coord = coords.split(" ");
					if(coord.length==2)
					{
						attach.latitude = Float.parseFloat(coord[0]);
						attach.longtitude = Float.parseFloat(coord[1]);
						attach.id = (int) (attach.longtitude*10000 + attach.latitude*100000);
						message.attachments.add(attach);
					}
				}
				if(jsonMessage.has("attachments") || jsonMessage.has("attachment"))
				{
					if(message.attachments==null)
						message.attachments = new ArrayList<Attachment>();
					
					JSONArray jsonAttachments = jsonMessage.getJSONArray("attachments");
					for(int j =0;j<jsonAttachments.length();j++)
					{
						JSONObject jsonAttachment = jsonAttachments.getJSONObject(j);
						String type = jsonAttachment.getString("type");
						if(type.equals("photo"))
						{
							JSONObject jsonPhoto = jsonAttachment.getJSONObject("photo");
							PhotoAttach photo = new PhotoAttach();
							photo.id = jsonPhoto.getInt("pid");
							photo.owner_id = jsonPhoto.getInt("owner_id");
							photo.photo_src = jsonPhoto.getString("src");
							if(jsonPhoto.has("src_xbig"))
								photo.photo_src_big = jsonPhoto.getString("src_xbig");
							else if (jsonPhoto.has("src_xbig"))
								photo.photo_src_big = jsonPhoto.getString("src_xbig");
							else
								photo.photo_src_big = jsonPhoto.getString("src_big");
							message.attachments.add(photo);
						}
						else if(type.equals("audio"))
						{
							JSONObject jsonAudio = jsonAttachment.getJSONObject("audio");
							AudioAttach audio = new AudioAttach();
							audio.id = jsonAudio.getInt("aid");
							audio.owner_id = jsonAudio.getInt("owner_id");
							audio.performer = jsonAudio.getString("performer");
							audio.url = jsonAudio.getString("url");
							audio.duration = jsonAudio.getInt("duration");
							audio.title = jsonAudio.getString("title");
							message.attachments.add(audio);
						}
						else if(type.equals("video"))
						{
							JSONObject jsonVideo = jsonAttachment.getJSONObject("video");
							VideoAttach video = new VideoAttach();
							video.id = jsonVideo.getInt("vid");
							video.owner_id = jsonVideo.getInt("owner_id");
							video.image = jsonVideo.getString("image");
							video.title = jsonVideo.getString("title");
							video.duration = jsonVideo.getInt("duration");
							message.attachments.add(video);
							
						}
						else if(type.equals("wall"))
						{
							
						}
						else if(type.equals("doc"))
						{
							JSONObject jsonDoc = jsonAttachment.getJSONObject("doc");
							DocAttach doc = new DocAttach();
							doc.id = jsonDoc.getInt("did");
							doc.owner_id = jsonDoc.getInt("owner_id");
							doc.title = jsonDoc.getString("title");
							doc.size = jsonDoc.getLong("size");
							doc.ext = jsonDoc.getString("ext");
							doc.url = jsonDoc.getString("url");
							message.attachments.add(doc);
						}
					}
				}
				
				if(jsonMessage.has("fwd_messages"))
				{
					JSONArray jsonFwd = jsonMessage.getJSONArray("fwd_messages");
					message.fwd_messages = parseGetMessagesResponse(jsonFwd.toString(), ownId, true,false);
				}
				result.add(message);
			}
			return result;
		} catch (JSONException e) {
			throw new JsonParseException(ErrorCode.ParsingError);
		}		
	}

	public static List<User> parseGetUsersResponse(String jsonString) throws JsonParseException
	{
		return parseGetUsersResponse(jsonString, false);
	}
	/**
	 * @param jsonString
	 * @return Signup SID string
	 * @throws JsonParseException - if any error was happen
	 */
	public static List<User> parseGetUsersResponse(String jsonString,boolean useHints) throws JsonParseException
	{
		try {
			ArrayList<User> result = new ArrayList<User>();
			JSONObject json = new JSONObject(jsonString);
			if(json.has("error"))
				throw new JsonParseException(ErrorCode.getByServerId(json.getJSONObject("error").getInt("error_code")));
			JSONArray jsonMessages = json.getJSONArray("response");
			
			for(int i = 0; i<jsonMessages.length();i++)
			{
				if(jsonMessages.get(i) instanceof Integer)
					continue;
				JSONObject jsonMessage = jsonMessages.getJSONObject(i);
				User user = new User();
				user.uid = jsonMessage.getInt("uid");
				user.first_name = jsonMessage.getString("first_name");
				user.last_name = jsonMessage.getString("last_name");
//				user.short_name = jsonMessage.getString("screen_name");
				user.photo = jsonMessage.getString(VKApplication.getInstance().getAvatarSize());
				user.online = jsonMessage.getInt("online")>0;
				if(jsonMessage.has("sex"))
					user.sex = jsonMessage.getInt("sex");
				if(jsonMessage.has("last_seen") && jsonMessage.getJSONObject("last_seen").has("time"))
					user.last_seen = jsonMessage.getJSONObject("last_seen").getLong("time");
				if(useHints)
					user.hintpos = i;
				if(jsonMessage.has("phone"))
					user.phones = jsonMessage.getString("phone");
				if(jsonMessage.has("has_mobile"))
					user.has_mobile = jsonMessage.getInt("has_mobile")>0;
				if(jsonMessage.has("photo_big"))
					user.photo_medium = jsonMessage.getString("photo_big");
				result.add(user);
			}
			return result;
		} catch (JSONException e) {
			throw new JsonParseException(ErrorCode.ParsingError);
		}		
	}
	
	   /**
     * @param jsonString
     * @return Signup SID string
     * @throws JsonParseException - if any error was happen
     */
    public static List<User> parseSearchDialogsResponse(String jsonString) throws JsonParseException
    {
        try {
            ArrayList<User> result = new ArrayList<User>();
            JSONObject json = new JSONObject(jsonString);
            if(json.has("error"))
                throw new JsonParseException(ErrorCode.getByServerId(json.getJSONObject("error").getInt("error_code")));
            JSONArray jsonMessages = json.getJSONArray("response");
            
            for(int i = 0; i<jsonMessages.length();i++)
            {
                if(jsonMessages.get(i) instanceof Integer)
                    continue;
                
                JSONObject jsonMessage = jsonMessages.getJSONObject(i);
                boolean isChat = jsonMessage.getString("type").equals("chat");
                User user = new User();
                if(isChat) {
                    user.uid = -jsonMessage.getInt("chat_id");
                    user.first_name = jsonMessage.getString("title");
                    user.last_name = "";
                    user.photo_bitmap = BitmapFactory.decodeResource(VKApplication.getInstance().getResources(), R.drawable.ic_photo_group);
                } else {
                    user.uid = jsonMessage.getInt("uid");
                    user.first_name = jsonMessage.getString("first_name");
                    user.last_name = jsonMessage.getString("last_name");
    //              user.short_name = jsonMessage.getString("screen_name");
                    user.photo = jsonMessage.getString(VKApplication.getInstance().getAvatarSize());
                    user.online = jsonMessage.getInt("online")>0;
                    if(jsonMessage.has("sex"))
                        user.sex = jsonMessage.getInt("sex");
                    if(jsonMessage.has("last_seen") && jsonMessage.getJSONObject("last_seen").has("time"))
                        user.last_seen = jsonMessage.getJSONObject("last_seen").getLong("time");
                    if(jsonMessage.has("phone"))
                        user.phones = jsonMessage.getString("phone");
                    if(jsonMessage.has("has_mobile"))
                        user.has_mobile = jsonMessage.getInt("has_mobile")>0;
                    if(jsonMessage.has("photo_big"))
                        user.photo_medium = jsonMessage.getString("photo_big");
                }
                result.add(user);
            }
            return result;
        } catch (JSONException e) {
            throw new JsonParseException(ErrorCode.ParsingError);
        }       
    }
	/**
	 * @param jsonString
	 * @return Signup SID string
	 * @throws JsonParseException - if any error was happen
	 */
	public static LongPollServer parseLongPollServerResponse(String jsonString) throws JsonParseException
	{
		try {
			JSONObject json = new JSONObject(jsonString);
			if(json.has("error"))
				throw new JsonParseException(ErrorCode.getByServerId(json.getJSONObject("error").getInt("error_code")));
			JSONObject jsonMessage = json.getJSONObject("response");
			LongPollServer result = new LongPollServer();
			result.key = jsonMessage.getString("key");
			result.server = jsonMessage.getString("server");
			result.ts = String.valueOf(jsonMessage.getLong("ts"));
			return result;			
		} catch (JSONException e) {
			throw new JsonParseException(ErrorCode.ParsingError);
		}		
	}
 
	/**
	 * @param jsonString
	 * @return Signup SID string
	 * @throws JsonParseException - if any error was happen
	 */
	public static String parseMessagesUploadServerResponse(String jsonString) throws JsonParseException
	{
		try {
			JSONObject json = new JSONObject(jsonString);
			if(json.has("error"))
				throw new JsonParseException(ErrorCode.getByServerId(json.getJSONObject("error").getInt("error_code")));
			JSONObject jsonMessage = json.getJSONObject("response");
			return jsonMessage.getString("upload_url");			
		} catch (JSONException e) {
			throw new JsonParseException(ErrorCode.ParsingError);
		}		
	}
	
	/**
	 * @param jsonString
	 * @return Signup SID string
	 * @throws JsonParseException - if any error was happen
	 */
	public static String parseMessagesUploadFileId(String jsonString) throws JsonParseException
	{
		try {
			JSONObject json = new JSONObject(jsonString);
			if(json.has("error"))
				throw new JsonParseException(ErrorCode.getByServerId(json.getJSONObject("error").getInt("error_code")));
			JSONArray jsonMessage = json.getJSONArray("response");
			return jsonMessage.getJSONObject(0).getString("id");		
		} catch (JSONException e) {
			throw new JsonParseException(ErrorCode.ParsingError);
		}		
	}
	
	/**
	 * @param jsonString
	 * @return Signup SID string
	 * @throws JsonParseException - if any error was happen
	 */
	public static String parseProfilePhotoSrc(String jsonString) throws JsonParseException
	{
		try {
			JSONObject json = new JSONObject(jsonString);
			if(json.has("error"))
				throw new JsonParseException(ErrorCode.getByServerId(json.getJSONObject("error").getInt("error_code")));
			JSONObject jsonMessage = json.getJSONObject("response");
			return jsonMessage.getString("photo_src");		
		} catch (JSONException e) {
			throw new JsonParseException(ErrorCode.ParsingError);
		}		
	}
	/**
	 * @param jsonString
	 * @return Signup SID string
	 * @throws JsonParseException - if any error was happen
	 */
	public static ServerUploadFile parseUploadedFile(String jsonString) throws JsonParseException
	{
		try {
			JSONObject json = new JSONObject(jsonString);
			if(json.has("error"))
				throw new JsonParseException(ErrorCode.getByServerId(json.getJSONObject("error").getInt("error_code")));
			ServerUploadFile result = new ServerUploadFile();
			result.server = json.getString("server");
		//	JSONArray photo = new JSONArray(json.getString("photo"));
			result.photo = json.getString("photo");
			result.hash = json.getString("hash");
			
			return result;		
		} catch (JSONException e) {
			throw new JsonParseException(ErrorCode.ParsingError);
		}		
	}
	
	/**
	 * @param jsonString
	 * @return Signup SID string
	 * @throws JsonParseException - if any error was happen
	 */
	public static LongPollUpdate parseLongPollResponse(String jsonString) throws JsonParseException
	{
		try {
			JSONObject json = new JSONObject(jsonString);
			if(json.has("failed"))
				throw new JsonParseException(ErrorCode.LongPollKeyNeedToBeUpdated);
			LongPollUpdate result = new LongPollUpdate();
			result.ts = String.valueOf(json.getLong("ts"));
			JSONArray jsonUpdates = json.getJSONArray("updates");
			result.updates = new ArrayList<LongPollUpdate.Update>();
			for(int i =0;i<jsonUpdates.length();i++)
			{
				JSONArray jsonUpdate = jsonUpdates.getJSONArray(i);
				LongPollUpdate.Update update = result.new Update();
				update.type = jsonUpdate.getInt(0);
				update.id = jsonUpdate.getInt(1);
				
				if(update.type==4)
				{
					update.flags = jsonUpdate.getInt(2);
					update.from_id = jsonUpdate.getInt(3);
					update.timestamp = jsonUpdate.getLong(4);
					update.title = jsonUpdate.getString(5);
					update.text = jsonUpdate.getString(6);
					if(jsonUpdate.length()>7)
					{
						JSONObject more = jsonUpdate.getJSONObject(7);
						if(update.from_id>=2000000000)
						{
							update.from_in_chat = more.getInt("from");
						}
						if(more.has("fwd"))
							update.haveFwd = true;
						int attachNum = 1;
						while(more.has("attach"+attachNum+"_type"))
						{
							if(update.attachments==null)
								update.attachments = new ArrayList<Attachment>();
							String type = more.getString("attach"+(attachNum)+"_type");
							String id = more.getString("attach"+(attachNum));
							if(type.equals("photo"))
							{
								PhotoAttach attach = new PhotoAttach();
								attach.id = Integer.parseInt(id.split("_")[1]);
								update.attachments.add(attach);
							}
							else if(type.equals("video"))
							{
								VideoAttach attach = new VideoAttach();
								attach.id = Integer.parseInt(id.split("_")[1]);
								update.attachments.add(attach);							
							}
							else if(type.equals("audio"))
							{
								AudioAttach attach = new AudioAttach();
								attach.id = Integer.parseInt(id.split("_")[1]);
								update.attachments.add(attach);
							}
							else if(type.equals("doc"))
							{
								DocAttach attach = new DocAttach();
								attach.id = Integer.parseInt(id.split("_")[1]);
								update.attachments.add(attach);
							}
							attachNum++;
						}
					}
				}
				result.updates.add(update);
			}
			return result;		
		} catch (JSONException e) {
			throw new JsonParseException(ErrorCode.ParsingError);
		}		
	}
	

	/**
	 * @param jsonString
	 * @return Signup SID string
	 * @throws JsonParseException - if any error was happen
	 */
	public static LongPollUpdate parseLongPollHistory(String jsonString) throws JsonParseException
	{
		try {
			JSONObject json = new JSONObject(jsonString);
			if(json.has("failed") || json.has("error"))
				throw new JsonParseException(ErrorCode.LongPollKeyNeedToBeUpdated);
			LongPollUpdate result = new LongPollUpdate();
			json = json.getJSONObject("response");
			JSONArray jsonUpdates = json.getJSONArray("history");
			result.updates = new ArrayList<LongPollUpdate.Update>();
			for(int i =0;i<jsonUpdates.length();i++)
			{
				JSONArray jsonUpdate = jsonUpdates.getJSONArray(i);
				LongPollUpdate.Update update = result.new Update();
				update.type = jsonUpdate.getInt(0);
				update.id = jsonUpdate.getInt(1);
				
				if(update.type==4)
				{
					update.flags = jsonUpdate.getInt(2);
					update.from_id = jsonUpdate.getInt(3);
				}
				result.updates.add(update);
			}
			JSONArray jsonMessages = json.getJSONArray("messages");
			List<Message> messages = parseGetMessagesResponse(jsonMessages.toString(), 0, false,true);
			for(int i = 0;i<result.updates.size();i++)
			{
				Update update = result.updates.get(i);
				if(update.type!=4)
					continue;
				for(int j = 0;j<messages.size();j++)
				{
					Message message = messages.get(j);
					if(update.id != message.mid)
						continue;
					if(!message.read_state)
						update.flags |= 1;
					if(message.out)
						update.flags |= 2;
					update.timestamp = message.date;
					update.title = message.title;
					update.text = message.body;
					update.attachments = message.attachments;
					update.haveFwd = message.fwd_messages!=null;
				}
			}
			return result;		
		} catch (JSONException e) {
			throw new JsonParseException(ErrorCode.ParsingError);
		}		
	}
	
	public static List<Integer> parseGetRequestsResponse(String jsonString) throws JsonParseException
	{
		try {
			List<Integer> result = new ArrayList<Integer>();
			JSONObject json = new JSONObject(jsonString);
			if(json.has("error"))
				throw new JsonParseException(ErrorCode.getByServerId(json.getJSONObject("error").getInt("error_code")));
			JSONArray arr =  json.getJSONArray("response");
			for(int i = 0;i<arr.length();i++)
				result.add(arr.getInt(i));
			return result;
		} catch (JSONException e) {
			throw new JsonParseException(ErrorCode.ParsingError);
		}		
	}
	
	public static HashMap<Integer,String> parseVideoGetResponse(String jsonString) throws JsonParseException
	{
		try {
			HashMap<Integer,String> result = new HashMap<Integer,String>();
			JSONObject json = new JSONObject(jsonString);
			if(json.has("error"))
				throw new JsonParseException(ErrorCode.getByServerId(json.getJSONObject("error").getInt("error_code")));
			JSONArray arr =  json.getJSONArray("response");
			for(int i = 1;i<arr.length();i++)
			{
				JSONObject files = arr.getJSONObject(i).getJSONObject("files");
				if(files.has("external"))
				{
					result.put(-1, files.getString("external"));
					return result;
				}
				Iterator<String> key = files.keys();
				
				while(key.hasNext())
				{
					String k = key.next();
					try {
					int size = Integer.parseInt(k.split("_")[1]);
					result.put(size, files.getString(k));
					} catch (Exception e) { continue; }
				}
			}
			return result;
		} catch (JSONException e) {
			throw new JsonParseException(ErrorCode.ParsingError);
		}		
	}
	/**
	 * @param jsonString
	 * @return Signup SID string
	 * @throws JsonParseException - if any error was happen
	 */
	public static Integer parseSendMessageResponse(String jsonString, Message msg) throws JsonParseException
	{
		try {
		    Log.d(VKApplication.TAG, "Send msg response : " + jsonString);
			JSONObject json = new JSONObject(jsonString);
			if(json.has("error")) {
			    if(json.getJSONObject("error").getInt("error_code")==14)
			        throw new CaptchaNeededException(ErrorCode.CaptchaNeeded, msg, json.getJSONObject("error").getString("captcha_sid"), json.getJSONObject("error").getString("captcha_img"));
				throw new JsonParseException(ErrorCode.getByServerId(json.getJSONObject("error").getInt("error_code")), msg.guid);
			}
			return json.getInt("response");
		} catch (JSONException e) {
			throw new JsonParseException(ErrorCode.ParsingError);
		}		
	}


	public static Boolean parseSuccessResponse(String jsonString) throws JsonParseException {
		try {
			JSONObject json = new JSONObject(jsonString);
			if(json.has("error"))
				throw new JsonParseException(ErrorCode.getByServerId(json.getJSONObject("error").getInt("error_code")));
			return json.getInt("response")>0;
		} catch (JSONException e) {
			throw new JsonParseException(ErrorCode.ParsingError);
		}	
	}

	public static Boolean parseSuccessMessageDeleteResponse(String jsonString) throws JsonParseException {
		try {
			JSONObject json = new JSONObject(jsonString);
			if(json.has("error"))
				throw new JsonParseException(ErrorCode.getByServerId(json.getJSONObject("error").getInt("error_code")));
			return json.has("response");
		} catch (JSONException e) {
			throw new JsonParseException(ErrorCode.ParsingError);
		}	
	}

	public static Boolean parseSignupConfirmResponse(String jsonString) throws JsonParseException {
		try {
			JSONObject json = new JSONObject(jsonString);
			if(json.has("error"))
				throw new JsonParseException(ErrorCode.getByServerId(json.getJSONObject("error").getInt("error_code")));
			json = json.getJSONObject("response");
			return json.getInt("success")>0;
		} catch (JSONException e) {
			throw new JsonParseException(ErrorCode.ParsingError);
		}	
	}
}
