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

public class FriendListAdapter extends ArrayAdapter<User> implements Filterable,SectionIndexer
{
	
	List<User> items;
	List<User> filteredItems;
	private boolean onlineMode;
	
	public FriendListAdapter(Context context, List<User> recordList)
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
	
	public FriendListAdapter(Context context, User[] recordList)
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
			row  = inflater.inflate(R.layout.item_friend_list, parent, false);
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
		
		wrapper.populateFrom(filteredItems.get(position), generateSections ? sectionsPositions.contains(position) : false);
		return row;
	}
	
	public void setMode(boolean online)
	{
		onlineMode = online;
	}
	Filter filter;
	@Override
	public Filter getFilter() {
		if(filter==null)
		{
			filter = new Filter() {
	
				@Override
				protected FilterResults performFiltering(CharSequence constraint) {
					boolean generateHeaders = false;
					FilterResults result = new FilterResults();				
					ArrayList<User> filteredValues = new ArrayList<User>();
					if((constraint==null || constraint.equals("")) && !onlineMode)
					{
						generateHeaders = true;
						filteredValues = (ArrayList<User>) items;
					}
					else if((constraint==null || constraint.equals("")) && onlineMode)
					{
						for(User user : items)
						{
							if(user.online)
								filteredValues.add(user);
						}
						generateHeaders = true;
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
					if(generateHeaders)
						generateSections(filteredValues);
					else
						sectionsPositions.clear();
					return result;
				}
	
				@Override
				protected void publishResults(CharSequence constraint,
						FilterResults results) {
					filteredItems = (List<User>) results.values;
					FriendListAdapter.this.notifyDataSetChanged();
				}
				
			};
		}
		return filter;
	}
	
	HashMap<Character, Integer> sectionsMap;
	HashSet<Integer> sectionsPositions = new HashSet<Integer>();
	ArrayList<Character> sectionsArray = new ArrayList<Character>();
	ArrayList<Integer> sectionsPos = new ArrayList<Integer>();
	int prevListSize;
    private boolean generateSections = true;
	private void generateSections(ArrayList<User> values)
	{
		if(values==null)
			return;
		if(sectionsMap==null)
			sectionsMap = new HashMap<Character, Integer>();
		if(sectionsPositions==null)
			sectionsPositions = new HashSet<Integer>();
		int size = values.size();
		if(prevListSize==size || size==0)
			return;
		prevListSize = size;
		sectionsMap.clear();
		sectionsPositions.clear();
		sectionsArray.clear();
		sectionsPos.clear();
		for(int i = size-1;i>=0;i--)
		{
			User item = values.get(i);
			if(item.hintpos<5)
				continue;
			sectionsMap.put(item.first_name.charAt(0), i);
		}
		char prevChar;
		prevChar = values.get(size-1).first_name.charAt(0);
		int sectionNum = 0;
		sectionsArray.add(' ');
		for(int i = 0;i<size;i++)
		{
			User item = values.get(i);
			if(item.hintpos<5)
			{
				sectionsPos.add(sectionNum);
				continue;
			}
			if(prevChar!=item.first_name.charAt(0))
			{
				sectionsArray.add(item.first_name.charAt(0));
				sectionNum++;
			}
			sectionsPos.add(sectionNum);
			prevChar=item.first_name.charAt(0);			
		}
		for(Integer pos : sectionsMap.values())
		{
			if(pos==0)
				continue;
			sectionsPositions.add(pos);
		}
	}

	@Override
	public int getPositionForSection(int section) {
		for(int i =0;i<sectionsPos.size();i++)
			if(sectionsPos.get(i)==section)
				return i;
		return 0;
//		return sectionsPositions.it;
	}

	@Override
	public int getSectionForPosition(int position) {
		    if(sectionsPos.size()<=position)
		        return 0;
		return sectionsPos.get(position);
	}

	@Override
	public Object[] getSections() {
		return sectionsArray.toArray(new Character[sectionsArray.size()]);
	}
	
	
	public void setGenerateSections(boolean value) {
	    this.generateSections = value;
	}
}
