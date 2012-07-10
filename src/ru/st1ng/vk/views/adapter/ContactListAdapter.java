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
import ru.st1ng.vk.activity.ContactInfoActivity;
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

public class ContactListAdapter extends ArrayAdapter<ContactName> implements Filterable,SectionIndexer
{

	List<ContactName> items;
	List<ContactName> filteredItems;
	
	
	
	public ContactListAdapter(Context context, List<ContactName> recordList)
	{
		super(context, R.layout.item_conversation_list, recordList);
		this.items = recordList;
		
		filteredItems = new ArrayList<ContactName>();
		if(items==null)
			return;
		for(ContactName user : items)
		{
			filteredItems.add(user);
		}
		generateSections((ArrayList<ContactName>) filteredItems);
	}
	
	public ContactListAdapter(Context context, ContactName[] recordList)
	{
		super(context, R.layout.item_conversation_list, recordList);
		this.items = new ArrayList<ContactName>();
		
		filteredItems = new ArrayList<ContactName>();
		for(ContactName user : recordList)
		{
			items.add(user);
			filteredItems.add(user);
		}
		
	}
	
	@Override
	public void notifyDataSetChanged() {
        generateSections((ArrayList<ContactName>) filteredItems);		
		super.notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		if(filteredItems==null)
			return 0;
		return filteredItems.size();
	}
	
	public void setItems(ContactName[] recordList)
	{
		items.clear();
		filteredItems.clear();
		for(ContactName user : recordList)
		{
			items.add(user);
			filteredItems.add(user);
		}		
	}

	public void setItems(List<ContactName> recordList)
	{
		items = recordList;
		filteredItems = recordList;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent)
	{
		View row = convertView;
		ContactWrapper wrapper;
		if(row==null)
		{
			LayoutInflater inflater = ((Activity)getContext()).getLayoutInflater();
			row  = inflater.inflate(R.layout.item_contact_list, parent, false);
			TextView dialogTitle = (TextView) row.findViewById(R.id.dialogTitle);
			dialogTitle.setTypeface(FontsUtil.MyRiad,Typeface.BOLD);
			wrapper = new ContactWrapper(row, getContext());
			row.setTag(wrapper);
			row.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(getContext(), ContactInfoActivity.class);
					intent.putExtra(ContactInfoActivity.EXTRA_CONTACTID, ((ContactWrapper)v.getTag()).contactId);
					getContext().startActivity(intent);								
				}
			});
		}		
		else
		{
			wrapper = (ContactWrapper) row.getTag();
		}
		
		wrapper.populateFrom(filteredItems.get(position),sectionsPositions.contains(position));
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
					boolean generateHeaders = false;
					FilterResults result = new FilterResults();				
					List<ContactName> filteredValues = new ArrayList<ContactName>();
					if((constraint==null || constraint.equals("")))
					{
						generateHeaders = true;
						filteredValues = items;
					}
					else
					{
						String[] names = constraint.toString().toLowerCase().split(" ");
						for(ContactName user : items)
						{
							if(user.contact_name==null)
								continue;
							for(String name : names)
							{
								if(user.first_name.toLowerCase().contains(name))
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
	
				@SuppressWarnings("unchecked")
				@Override
				protected void publishResults(CharSequence constraint,
						FilterResults results) {
					filteredItems = (List<ContactName>) results.values;
					ContactListAdapter.this.notifyDataSetChanged();
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
	private void generateSections(List<ContactName> values)
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
			sectionsMap.put(((ContactName) item).contact_name.charAt(0), i);
		}
		char prevChar;
		prevChar = ((ContactName)values.get(size-1)).contact_name.charAt(0);
		int sectionNum = 0;
		sectionsArray.add(' ');
		for(int i = 0;i<size;i++)
		{
			ContactName item = values.get(i);
				if(prevChar!=((ContactName)item).contact_name.charAt(0))
				{
					sectionsArray.add(((ContactName)item).contact_name.charAt(0));
					sectionNum++;
				}
				sectionsPos.add(sectionNum);
				prevChar=((ContactName)item).contact_name.charAt(0);			
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
