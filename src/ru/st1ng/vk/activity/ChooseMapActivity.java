package ru.st1ng.vk.activity;

import java.util.List;

import ru.st1ng.vk.R;
import ru.st1ng.vk.map.AddressOverlay;
import ru.st1ng.vk.map.AddressOverlay.OnPositionListener;
import ru.st1ng.vk.util.FontsUtil;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.renderscript.Type;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class ChooseMapActivity extends MapActivity {

	List<Overlay> overlays;
	MapView mapView;

	AddressOverlay overlay;
	GeoPoint point;
	Button okButton;
	Button cancelButton;
	
	public static final String LONGTITUDE = "longtitude";
	public static final String LATITUDE = "latitude";
	
	TextView titleText;
	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.screen_map);
		titleText = (TextView) findViewById(R.id.titleText);
		mapView = (MapView) findViewById(R.id.mapview);
		okButton = (Button) findViewById(R.id.okButton);
		cancelButton = (Button) findViewById(R.id.cancelButton);
		titleText.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
		okButton.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
        cancelButton.setTypeface(FontsUtil.MyRiad, Typeface.BOLD);
        
		okButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent result = new Intent();
				result.putExtra(LONGTITUDE, point.getLongitudeE6());
				result.putExtra(LATITUDE, point.getLatitudeE6());
				setResult(RESULT_OK, result);
				finish();
			}
		});
		cancelButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		mapView.setBuiltInZoomControls(true);
		overlays = mapView.getOverlays();
		overlay = new AddressOverlay(BitmapFactory.decodeResource(getResources(), R.drawable.im_map_location));
		
		overlay.setOnPositionListener(new OnPositionListener() {
			
			@Override
			public void OnPosition(GeoPoint pos) {
				point = pos;
				okButton.setEnabled(true);
			}
		});
		overlays.add(overlay);

	}
	
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

}
