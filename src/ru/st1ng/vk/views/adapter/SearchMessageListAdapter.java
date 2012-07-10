/**
 * FieldListAdapter is the...
 * TODO: Please complete the overall description of this class
 * TODO: Please comment all methods within this class using the JavaDoc format
 * 
 * @author Surojit Pakira
 * @version v1.2
 * @since Manage My Pain v1.0
 */

package ru.st1ng.vk.views.adapter;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import ru.st1ng.vk.R;
import ru.st1ng.vk.activity.ConversationActivity;
import ru.st1ng.vk.activity.MessagesActivity;
import ru.st1ng.vk.model.Message;
import ru.st1ng.vk.model.User;
import ru.st1ng.vk.util.FontsUtil;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class SearchMessageListAdapter extends ArrayAdapter<Message>
{
	
	List<Message> items;
	public AtomicBoolean scrollingNow;

	public SearchMessageListAdapter(Context context, List<Message> recordList)
	{
		super(context, R.layout.item_conversation_list, recordList);
		this.items = recordList;
		scrollingNow = new AtomicBoolean(false);
	}
	
	
	@Override
	public int getCount() {
		return items.size();
	}
	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent)
	{
		View row = convertView;
		DialogWrapper wrapper;
		if(row==null)
		{
			LayoutInflater inflater = ((Activity)getContext()).getLayoutInflater();
			row  = inflater.inflate(R.layout.item_friend_list, parent, false);
			TextView dialogTitle = (TextView) row.findViewById(R.id.dialogTitle);
			dialogTitle.setTypeface(FontsUtil.MyRiad,Typeface.BOLD);
			TextView dialogBody = (TextView) row.findViewById(R.id.dialogBody);
			dialogBody.setTypeface(FontsUtil.Roboto);
			wrapper = new DialogWrapper(row, getContext(), scrollingNow);
		
			row.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					//Dirty hack
					Intent intent = new Intent(getContext(), ConversationActivity.class);
					if(((DialogWrapper)v.getTag()).chatId>0)
						intent.putExtra(ConversationActivity.EXTRA_CHATID, -((DialogWrapper)v.getTag()).chatId);
					else
						intent.putExtra(ConversationActivity.EXTRA_USERID, ((DialogWrapper)v.getTag()).userId);
					getContext().startActivity(intent);			
				}
			});
			row.setTag(wrapper);
		}		
		else
		{
			wrapper = (DialogWrapper) row.getTag();
		}
		
		wrapper.populateFrom(items.get(position));
		return row;
	}
}
