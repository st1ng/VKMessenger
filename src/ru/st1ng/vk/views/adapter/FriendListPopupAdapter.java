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
import java.util.Collections;
import java.util.List;

import ru.st1ng.vk.R;
import ru.st1ng.vk.activity.ContactsActivity;
import ru.st1ng.vk.activity.ConversationActivity;
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
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

public class FriendListPopupAdapter extends ArrayAdapter<User> implements Filterable
{
	
	List<User> items;
	List<User> filteredItems;
	public FriendListPopupAdapter(Context context, List<User> recordList)
	{
		super(context, R.layout.item_conversation_list, recordList);
		this.items = recordList;
		filteredItems = new ArrayList<User>();
		for(User user : items)
		{
			filteredItems.add(user);
		}
	}
	
	public FriendListPopupAdapter(Context context, User[] recordList)
	{
		super(context, R.layout.item_conversation_list, recordList);
		this.items = new ArrayList<User>();
		
		filteredItems = new ArrayList<User>();
		for(User user : recordList)
		{
			items.add(user);
			filteredItems.add(user);
		}
		
	}
	
	@Override
	public void notifyDataSetChanged() {
		
		super.notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		if(filteredItems==null)
			return 0;
		return filteredItems.size();
	}
	
	public void setItems(User[] recordList)
	{
		items.clear();
		filteredItems.clear();
		for(User user : recordList)
		{
			items.add(user);
			filteredItems.add(user);
		}		
	}

	public void setItems(List<User> recordList)
	{
		items = recordList;
		filteredItems = recordList;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent)
	{
		View row = convertView;
		FriendWrapper wrapper;
		if(row==null)
		{
			LayoutInflater inflater = ((Activity)getContext()).getLayoutInflater();
			row  = inflater.inflate(R.layout.item_search_conversation_list, parent, false);
			TextView dialogTitle = (TextView) row.findViewById(R.id.dialogTitle);
			dialogTitle.setTypeface(FontsUtil.MyRiad,Typeface.BOLD);
			wrapper = new FriendWrapper(row, getContext());
			row.setTag(wrapper);
			row.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(getContext(), ConversationActivity.class);
					intent.putExtra(ConversationActivity.EXTRA_USERID, ((FriendWrapper)v.getTag()).userId);
					getContext().startActivity(intent);								
				}
			});
		}		
		else
		{
			wrapper = (FriendWrapper) row.getTag();
		}
		
		wrapper.populateFrom(filteredItems.get(position),false);
		return row;
	}
	
	Filter filter;
	@Override
	public Filter getFilter() {
		if(filter==null)
		{
			filter = new Filter() {
	
				@Override
				protected FilterResults performFiltering(CharSequence constraint) {
					FilterResults result = new FilterResults();				
					if(constraint==null)
					{
						return result;
					}
					ArrayList<User> filteredValues = new ArrayList<User>();
					if(constraint.equals("ALL"))
					{
						filteredValues = (ArrayList<User>) items;
					}
					else if(constraint.equals("ONLINE"))
					{
						for(User user : items)
						{
							if(user.online)
								filteredValues.add(user);
						}
					}
					else
					{
						String[] names = constraint.toString().toLowerCase().split(" ");
						for(User user : items)
						{
							if(user.first_name==null || user.last_name==null)
								continue;
							for(String name : names)
							{
								if(user.first_name.toLowerCase().contains(name)
										|| user.last_name.toLowerCase().contains(name))
								{
									filteredValues.add(user);
									break;
								}
							}
						}
					}
					result.values = filteredValues;
					return result;
				}
	
				@Override
				protected void publishResults(CharSequence constraint,
						FilterResults results) {
					filteredItems = (List<User>) results.values;
					FriendListPopupAdapter.this.notifyDataSetChanged();
				}
				
			};
		}
		return filter;
	}
}
