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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import ru.st1ng.vk.R;
import ru.st1ng.vk.activity.ContactsActivity;
import ru.st1ng.vk.activity.ConversationActivity;
import ru.st1ng.vk.activity.FriendRequestActivity;
import ru.st1ng.vk.model.ContactName;
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
import android.widget.SectionIndexer;
import android.widget.TextView;

public class SearchFriendListAdapter extends ArrayAdapter<User> implements Filterable,SectionIndexer
{
	
	List<User> items;
	List<User> filteredItems;
	private boolean onlineMode;
	
	public SearchFriendListAdapter(Context context, List<User> recordList)
	{
		super(context, R.layout.item_conversation_list, recordList);
		this.items = recordList;
		
		filteredItems = new ArrayList<User>();
		for(User user : items)
		{
			filteredItems.add(user);
		}
		generateSections((ArrayList<User>) filteredItems);
	}
	
	public SearchFriendListAdapter(Context context, User[] recordList)
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
		generateSections(filteredItems);
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
		SearchFriendWrapper wrapper;
		if(row==null)
		{
			LayoutInflater inflater = ((Activity)getContext()).getLayoutInflater();
			row  = inflater.inflate(R.layout.item_friend_list, parent, false);
			TextView dialogTitle = (TextView) row.findViewById(R.id.dialogTitle);
			dialogTitle.setTypeface(FontsUtil.MyRiad,Typeface.BOLD);
			wrapper = new SearchFriendWrapper(row, getContext());
			row.setTag(wrapper);
			row.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(getContext(), FriendRequestActivity.class);
					intent.putExtra(FriendRequestActivity.EXTRA_USERID, ((SearchFriendWrapper)v.getTag()).userId);
					intent.putExtra(FriendRequestActivity.EXTRA_REQUEST_IN, ((SearchFriendWrapper)v.getTag()).inrequest);
					getContext().startActivity(intent);								
				}
			});
		}		
		else
		{
			wrapper = (SearchFriendWrapper) row.getTag();
		}
		
		if(searchMode)
			wrapper.populateFrom(filteredItems.get(position),false, false, searchMode);
		else
			wrapper.populateFrom(filteredItems.get(position),requestsPos==position, suggestionsPos==position, searchMode);
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
					ArrayList<User> filteredValues = new ArrayList<User>();
					if((constraint==null || constraint.equals("")) && !onlineMode)
					{
						filteredValues = (ArrayList<User>) items;
					}
					else
					{
						String[] names = constraint.toString().toLowerCase().split(" ");
						for(User user : items)
						{
							if(user.first_name==null || user.last_name==null)
								continue;
							if(onlineMode && !user.online)
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
					generateSections(filteredValues);
					return result;
				}
	
				@Override
				protected void publishResults(CharSequence constraint,
						FilterResults results) {
					filteredItems = (List<User>) results.values;
					SearchFriendListAdapter.this.notifyDataSetChanged();
				}
				
			};
		}
		return filter;
	}
	
	private int requestsPos = -1;
	private int suggestionsPos = -1;
	private void generateSections(List<User> values)
	{
		if(values==null)
			return;
		int size = values.size();
		requestsPos = -1;
		suggestionsPos = -1;
		for(int i = size-1;i>=0;i--)
		{
			User item = values.get(i);
			if(item.request)
			{
				requestsPos = 0;
				if(i!=size-1)
					suggestionsPos = i+1;
				break;
			}
			suggestionsPos = 0;
		}
	}

	private boolean searchMode = false;
	public void setSearchMode(boolean search)
	{
		searchMode = search;
	}
	
	@Override
	public int getPositionForSection(int section) {
		return 0;
//		return sectionsPositions.it;
	}

	@Override
	public int getSectionForPosition(int position) {
		return 0;
	}

	@Override
	public Object[] getSections() {
		return null;
	}
}