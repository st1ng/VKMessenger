package ru.st1ng.vk.views.adapter;

import java.util.Calendar;
import java.util.concurrent.atomic.AtomicBoolean;

import ru.st1ng.vk.R;
import ru.st1ng.vk.VKApplication;
import ru.st1ng.vk.data.ImageCache;
import ru.st1ng.vk.model.Message;
import ru.st1ng.vk.model.User;
import ru.st1ng.vk.util.DateFormatUtil;
import ru.st1ng.vk.util.ImageUtil;
import ru.st1ng.vk.views.UserImageView;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SearchMessageWrapper {
		private TextView title=null;
		private TextView body=null;
		private TextView date = null;
		private UserImageView icon=null;
		private static Drawable selfDrawable;
		private View row=null;

		private Calendar recordCalendar;
		private Calendar currentCalendar;
		
		private Context context;
		
		public int userId;
		public int chatId;
		private AtomicBoolean scrollingNow;
		public SearchMessageWrapper(View row, Context context, AtomicBoolean scrollingNow) {
			this.row=row; //The View object that represents a single row
			this.context = context;
			this.scrollingNow = scrollingNow;
		}

		public void populateFrom(final Message r) { //Associate to the item components the value from the menu bean
			userId = r.uid;
			chatId = r.chat_id;
			if(r.user==null)
			{
				getTitle().setText(r.title);
				getTitle().setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
				getBody().setText(r.body);		
			}
			else
			{
				if(r.chat_id>0)
				{
					getTitle().setText(r.title);
					getTitle().setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_multichat, 0, 0, 0);
				}
				else
				{
					getTitle().setText(r.user.first_name + " " + r.user.last_name);
					if(r.user.online)
						getTitle().setCompoundDrawablesWithIntrinsicBounds(0, 0, r.user.online ? R.drawable.ic_online_list : 0, 0);
					else
						getTitle().setCompoundDrawablesWithIntrinsicBounds(0, 0, r.user.online ? R.drawable.ic_online_list : 0, 0);
				}
				
				if(r.out)
				{
					if(getSelfDrawable()!=null)
						getBody().setCompoundDrawables(getSelfDrawable(), null, null, null);
					else
						getBody().setCompoundDrawablesWithIntrinsicBounds(R.drawable.im_nophoto, 0, 0, 0);
					getBody().setMaxLines(1);
				}
				else
				{
					if(r.chat_id>0)
					{
						if(r.user!=null && r.user.photo_bitmap!=null)
						{
							if(r.user.photo_drawable==null)
							{
								
								r.user.photo_drawable = new BitmapDrawable(r.user.photo_bitmap);
								r.user.photo_drawable.setBounds(0, 0, getBody().getMeasuredHeight()-2, getBody().getMeasuredHeight()-2);
							}
							getBody().setCompoundDrawables(r.user.photo_drawable, null, null, null);
						}
						else
							getBody().setCompoundDrawablesWithIntrinsicBounds(R.drawable.im_nophoto, 0, 0, 0);
						getBody().setMaxLines(1);
					}
					else
					{
						getBody().setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
						getBody().setMaxLines(2);
					}
				}
				
				if(r.body.equals(""))
				{
        			if(r.attachments!=null)
        			{
        				getBody().setMaxLines(1);
        				getBody().setText((r.attachments.size()==1 ? context.getString(R.string.document) : context.getString(R.string.documents)) + "(" + r.attachments.size() +")");
        				
        			} else if (r.fwd_messages!=null && r.fwd_messages.size()>0)
        			{
                        getBody().setMaxLines(1);
                        getBody().setText(r.fwd_messages.size() + " " + context.getString(r.fwd_messages.size()==1 ? R.string.forwarded_message : R.string.forwarded_messages));
        			} else {
        			    getBody().setText(r.body);
        			}
				}
				else
				{
					getBody().setText(r.body);					
				}

			}
			getDate().setText(getDateString(context, r));
			
			if(r.read_state)
			{
				getBody().setBackgroundColor(0x0);
				row.setBackgroundResource(R.drawable.bg_conversation_selector);
			}
			else
			{
				if(r.out)
				{
					getBody().setBackgroundResource(R.drawable.bg_conversation_unread_selector);
					row.setBackgroundResource(R.drawable.bg_conversation_selector);
				}				
				else
				{
					getBody().setBackgroundColor(0x0);
					row.setBackgroundResource(R.drawable.bg_conversation_unread_selector);
				}
			}
			
			if(r.chat_id>0)
			{
				getIcon().setUsersFromDialog(r);
			}
			else
			{
				getIcon().setUser(r.user);
			}
			
		}

		private String getDateString(Context context, Message r)
		{
			Calendar current = Calendar.getInstance();
			Calendar record = getRecordCalendar();
			record.setTimeInMillis(r.date*1000);
			 

			if(record.get(Calendar.YEAR) == current.get(Calendar.YEAR))
			{
					if(record.get(Calendar.MONTH)==current.get(Calendar.MONTH))
					{
						if(record.get(Calendar.DATE) == current.get(Calendar.DATE)-1)
							return context.getString(R.string.yesterday);

						if(record.get(Calendar.DATE) == current.get(Calendar.DATE))
						{
							return DateFormatUtil.getTimeFormat(context).format(record.getTime());
						}
					}
					
			}
			return DateFormatUtil.getDateFormat(context).format(record.getTime());
		}
		
		TextView getTitle() {
			if (title==null) {
				title=(TextView)row.findViewById(R.id.dialogTitle);
			}
			return title;
		}

		TextView getBody() {
			if (body==null) {
				body=(TextView)row.findViewById(R.id.dialogBody);
			}
			return body;
		}

		TextView getDate() {
			if(date==null) {
				date = (TextView) row.findViewById(R.id.dialogDate);
			}
			return date;
		}
		
		UserImageView getIcon() {
			if (icon==null) {
				icon=(UserImageView)row.findViewById(R.id.dialogIcon);
				icon.setScrollingIndicator(scrollingNow);
			}
			return icon ;
		}

		Drawable getSelfDrawable() {
			User account = VKApplication.getInstance().getCurrentUser();
				if(selfDrawable==null && account.photo_bitmap!=null)
					selfDrawable = new BitmapDrawable(account.photo_bitmap);
				else if(selfDrawable==null && account.photo!=null && ImageCache.getInstance().isPhotoPresentForUser(account)) {								
						selfDrawable = new BitmapDrawable(ImageCache.getInstance().getPhotoForUser(account));
				}
				if(selfDrawable!=null && (selfDrawable.getBounds().bottom<30 || selfDrawable.getBounds().right<30))
				{
					selfDrawable.setBounds(0, 0, getBody().getMeasuredHeight()-2, getBody().getMeasuredHeight()-2);
				}
			return selfDrawable;
		}
		
		Calendar getRecordCalendar()
		{
			if(recordCalendar==null)
				recordCalendar = Calendar.getInstance();
			return recordCalendar;
		}

		Calendar getCurrentCalendar()
		{
			if(currentCalendar==null)
				currentCalendar = Calendar.getInstance();
			return currentCalendar;
		}
		

}
