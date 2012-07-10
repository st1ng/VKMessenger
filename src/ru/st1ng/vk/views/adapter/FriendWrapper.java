package ru.st1ng.vk.views.adapter;

import java.util.Calendar;

import ru.st1ng.vk.R;
import ru.st1ng.vk.data.ImageCache;
import ru.st1ng.vk.model.ContactName;
import ru.st1ng.vk.model.Message;
import ru.st1ng.vk.model.User;
import ru.st1ng.vk.util.FontsUtil;
import ru.st1ng.vk.views.UserImageView;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class FriendWrapper {
		private TextView title=null;
		private TextView body=null;
		private TextView date = null;
		private UserImageView icon=null;
		private ImageView online=null;
		private TextView dividerText;
		private View row=null;
		private TextView subtitle;

		private Context context;
		
		public int userId;
		
		public FriendWrapper(View row, Context context) {
			this.row=row; //The View object that represents a single row
			this.context = context;
		}

		public void populateFrom(User r, boolean isHeader) { //Associate to the item components the value from the menu bean
				userId = r.uid;
				getTitle().setText(r.first_name + " " + r.last_name);
					getOnline().setVisibility(r.online ? View.VISIBLE : View.INVISIBLE);
					row.setBackgroundResource(R.drawable.bg_conversation_selector);
				
				getIcon().setUser(r);
				if(isHeader)
				{
					getDivider().setVisibility(View.VISIBLE);
					getDivider().setText(r.first_name.charAt(0)+"");
	                getDivider().setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
				}
				else
				{
					getDivider().setVisibility(View.GONE);					
				}
		}

		private String getDateString(Message r)
		{
			Calendar current = Calendar.getInstance();
			Calendar record = Calendar.getInstance();
			record.setTimeInMillis(r.date*1000);
			java.text.DateFormat dateformat = android.text.format.DateFormat.getDateFormat(context);
			java.text.DateFormat timeformat = android.text.format.DateFormat.getTimeFormat(context);

			if(record.get(Calendar.YEAR) == current.get(Calendar.YEAR))
			{
					if(record.get(Calendar.MONTH)==current.get(Calendar.MONTH))
					{
						if(record.get(Calendar.DATE) == current.get(Calendar.DATE))
						{
							return timeformat.format(record.getTime());
						}
					}
					
			}
			return dateformat.format(record.getTime());
		}
		
		TextView getTitle() {
			if (title==null) {
				title=(TextView)row.findViewById(R.id.dialogTitle);
			}
			return title;
		}

		TextView getSubTitle() {
			if (subtitle==null) {
				subtitle=(TextView)row.findViewById(R.id.dialogSubtitle);
			}
			return subtitle;
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
			}
			return icon ;
		}
		
		ImageView getOnline() {
			if (online==null) {
				online=(ImageView)row.findViewById(R.id.dialogOnline);
			}
			return online ;
		}
		
		TextView getDivider() {
			if(dividerText == null)
				dividerText = (TextView) row.findViewById(R.id.dividerText);
			return dividerText;
		}
}
