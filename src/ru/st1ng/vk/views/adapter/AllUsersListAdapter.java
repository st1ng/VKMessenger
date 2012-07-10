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
import ru.st1ng.vk.activity.ComposeMessageActivity;
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

public class AllUsersListAdapter extends ArrayAdapter<User> implements Filterable,SectionIndexer
{
	
	List<User> items;
	List<User> filteredItems;
	private boolean onlineMode;
	private String forwardMessages;
	private boolean mode;
	public AllUsersListAdapter(Context context, List<User> recordList, String forwardMessages, boolean chooseMode)
	{
		super(context, R.layout.item_conversation_list, recordList);
		this.items = recordList;
		this.mode = chooseMode;
		filteredItems = new ArrayList<User>();
		for(User user : items)
		{
			filteredItems.add(user);
		}
		generateSections((ArrayList<User>) filteredItems);
		this.forwardMessages = forwardMessages;
	}
	
	public AllUsersListAdapter(Context context, User[] recordList)
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
					
					if(mode)
					{
						if(getContext() instanceof Activity)
						{
							Intent result = new Intent();
							result.putExtra(ComposeMessageActivity.EXTRA_RESULT_USERID, ((FriendWrapper)v.getTag()).userId);
							Activity activity = (Activity) getContext();
							activity.setResult(Activity.RESULT_OK, result);
							activity.finish();
						}						
					}
					else
					{
						Intent intent = new Intent(getContext(), ConversationActivity.class);
						intent.putExtra(ConversationActivity.EXTRA_USERID, ((FriendWrapper)v.getTag()).userId);
						if(forwardMessages!=null)
							intent.putExtra(ConversationActivity.EXTRA_FORWARD_MESSAGES, forwardMessages);
						getContext().startActivity(intent);		
						if(getContext() instanceof Activity)
						{
							((Activity)getContext()).finish();
						}
					}
				}
			});
		}		
		else
		{
			wrapper = (FriendWrapper) row.getTag();
		}
		
		wrapper.populateFrom(filteredItems.get(position),sectionsPositions.contains(position));
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
					AllUsersListAdapter.this.notifyDataSetChanged();
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
			sectionsMap.put(item.first_name.charAt(0), i);
		}
		char prevChar;
		prevChar = values.get(size-1).first_name.charAt(0);
		int sectionNum = 0;
		sectionsArray.add(' ');
		for(int i = 0;i<size;i++)
		{
			User item = values.get(i);
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
		
		return sectionsPos.get(position);
	}

	@Override
	public Object[] getSections() {
		return sectionsArray.toArray(new Character[sectionsArray.size()]);
	}
}
