package ru.st1ng.vk.model;

import android.graphics.Bitmap;
import ru.st1ng.vk.VKApplication;

public class GeoAttach extends Attachment {

	@Override
	public Type getType() {
		return Type.Geo;
	}
	
	public float latitude;
	
	public float longtitude;

	public String placeName;
	
	private static final String MAPURL = "http://maps.google.com/maps/api/staticmap?center=LAT,LONG&zoom=9&size=150x150&maptype=roadmap&sensor=true&language=LANG&markers=LAT,LONG";
	
	private String url;
	public String getUrl()
	{
		if(latitude==0 && longtitude==0)
			return null;
		if(url==null)
			url = MAPURL.replace("LAT", "" + latitude)
			.replace("LONG", "" + longtitude)
			.replace("LANG", VKApplication.getInstance().getAPILang());
		return url;
	}
}
