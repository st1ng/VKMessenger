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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
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
import android.widget.SectionIndexer;
import android.widget.TextView;

public class MessagesListAdapter extends ArrayAdapter<Message> implements SectionIndexer
{
	
	List<Message> items;

	public static final int TYPE_TEXT = 0;
	public static final int TYPE_ATTACH = 1;
	public boolean chat = false;
	ConversationActivity activity;
	
	private OnResendClickListener resendListener;
	public MessagesListAdapter(Context context, List<Message> recordList,boolean chat, ConversationActivity activity)
	{
		super(context, R.layout.item_message_list, recordList);
		this.items = recordList;
		this.chat = chat;
		this.activity = activity;
		this.sectionsPositions = new HashSet<Integer>();
	}
	
	public interface OnResendClickListener
	{
	    void OnResend(Message msg);
	}

	public void setOnResendClickListener(OnResendClickListener listener) {
	    resendListener = listener;
	}
	
	public List<Message> getItems() {
	    return this.items;
	}
	
	@Override
	public int getCount() {
		return items.size();
	}
	
	@Override
	public Message getItem(int position) {
		return items.get(position);
	}
	
	@Override
	public int getItemViewType(int position) {
		return items.get(position).attachments==null && items.get(position).fwd_messages==null ? TYPE_TEXT : TYPE_ATTACH;
	}
	
	@Override
	public int getViewTypeCount() {
		return 2;
	}
	
	@Override
	public void notifyDataSetChanged() {
		generateSections();
		super.notifyDataSetChanged();
	}
	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent)
	{
		View row = convertView;
		MessageWrapper wrapper;
		if(row==null)
		{
			LayoutInflater inflater = ((Activity)getContext()).getLayoutInflater();
			int type = getItemViewType(position);
			if(type==TYPE_TEXT)
			{
				row  = inflater.inflate(R.layout.item_message_list, parent, false);
			}
			else
			{
				row  = inflater.inflate(R.layout.item_message_attach_list, parent, false);
			}
			row.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
                    MessageWrapper wrapper = (MessageWrapper) v.getTag();
                    if(wrapper==null)
                        return;
                    Message msg = wrapper.getMessage();
                    if(msg!=null && msg.networkError && resendListener!=null) {
                        resendListener.OnResend(msg);
                        return;
                    }
                    
					((MessageWrapper)v.getTag()).toggle();
					if(((MessageWrapper)v.getTag()).isChecked())
						checkedItems.add(((MessageWrapper)v.getTag()).mid);
					else
						checkedItems.remove(((MessageWrapper)v.getTag()).mid);
					activity.onItemsCheckChanged();
					notifyDataSetChanged();
				}
			});
			row.findViewById(R.id.pendingSend).setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View v) {
                    MessageWrapper wrapper = (MessageWrapper) v.getTag();
                    if(wrapper==null)
                        return;
                    Message msg = wrapper.getMessage();
                    if(msg!=null && msg.networkError && resendListener!=null) {
                        resendListener.OnResend(msg);
                    }
                }
            });
			TextView dialogBody = (TextView) row.findViewById(R.id.messageText);
			dialogBody.setTypeface(FontsUtil.Roboto);
			TextView messageHeader = (TextView) row.findViewById(R.id.dateHeader);
			messageHeader.setTypeface(FontsUtil.MyRiad,Typeface.BOLD);
			wrapper = new MessageWrapper(row, getContext(), type,chat);
			row.findViewById(R.id.pendingSend).setTag(wrapper);
			row.setTag(wrapper);
		}		
		else
		{
			wrapper = (MessageWrapper) row.getTag();
		}
		
		wrapper.populateFrom(items.get(position),sectionsPositions.contains(position));
		return row;
	}

	
	HashSet<Long> checkedItems = new HashSet<Long>();
	
	public HashSet<Long> getCheckedItems()
	{
		return checkedItems;
	}
	
	public void clearCheckedItems()
	{
		checkedItems.clear();
		for(int i = 0;i<items.size();i++)
			items.get(i).setChecked(false);
		notifyDataSetChanged();
		activity.onItemsCheckChanged();
	}
	
	@Override
	public int getPositionForSection(int section) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public int getSectionForPosition(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String[] getSections() {

		return null;
	}
	
	Calendar cal;
	HashMap<String, Integer> sectionsMap;
	HashSet<Integer> sectionsPositions;
	int prevListSize;
	private void generateSections()
	{
		if(cal==null)
			cal = Calendar.getInstance();
		if(sectionsMap==null)
			sectionsMap = new HashMap<String, Integer>();
		if(sectionsPositions==null)
			sectionsPositions = new HashSet<Integer>();
		int size = items.size();
		if(prevListSize==size)
			return;
		prevListSize = size;
		sectionsMap.clear();
		sectionsPositions.clear();

		for(int i = size-1;i>=0;i--)
		{
			Message item = items.get(i);
			cal.setTimeInMillis(item.date*1000);
			if(item.dateString==null)
				item.dateString = getDateFormat().format(cal.getTime());
			sectionsMap.put(item.dateString, i);
		}
		for(Integer pos : sectionsMap.values())
		{
			if(pos==0)
				continue;
			sectionsPositions.add(pos);
		}
	}
	
	java.text.DateFormat dateFormat;
	java.text.DateFormat getDateFormat()
	{
		if(dateFormat==null)
			dateFormat = android.text.format.DateFormat.getDateFormat(getContext());
		return dateFormat;
	}

	
	
}
