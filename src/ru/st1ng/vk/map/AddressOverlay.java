package ru.st1ng.vk.map;

import ru.st1ng.vk.VKApplication;
import ru.st1ng.vk.model.GeoAttach;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

public class AddressOverlay extends Overlay implements OnGestureListener{
	GeoPoint pos;
	Bitmap bg;
	GestureDetector gesture;
	// BalloonView balloon;
	boolean created = true;
	MapView mapView;
	OnPositionListener listener;

	public AddressOverlay(Bitmap bg) {
		this.bg = bg;
		gesture = new GestureDetector(this);
	}

	public void setAddress(GeoPoint addr) {
		this.pos = addr;
	}

	@Override
	public boolean onTap(GeoPoint p, MapView mapView) {
		/*
		Point tap = new Point();
		mapView.getProjection().toPixels(p, tap);
		Point my = new Point();
		mapView.getProjection().toPixels(pos, my);

		 * if (balloon == null) {
		 * 
		 * balloon = new BalloonView(mapView.getContext(), 0, mapView); created
		 * = false;
		 * 
		 * MapView.LayoutParams params = new MapView.LayoutParams(
		 * LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, pos,
		 * MapView.LayoutParams.BOTTOM_CENTER); params.mode =
		 * MapView.LayoutParams.MODE_MAP; balloon.setLayoutParams(params);
		 * balloon.setData(this.address); mapView.addView(balloon); created =
		 * true; } int centerX = tap.x + bgVisited.getWidth() / 2; int centerY =
		 * tap.y + bgVisited.getHeight() / 2; boolean visible = Math.abs(centerX
		 * - (my.x + bgVisited.getWidth() / 2)) < bgVisited .getWidth() / 2 &&
		 * Math.abs(centerY - (my.y + bgVisited.getHeight() / 2)) < bgVisited
		 * .getHeight() / 2;
		 * 
		 * balloon.setVisibility(visible);
		 */
		return super.onTap(p, mapView);
	}

	
	@Override
	public boolean onTouchEvent(MotionEvent e, MapView mapView) {
		this.mapView = mapView;
		return gesture.onTouchEvent(e);
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		super.draw(canvas, mapView, shadow);
		if(pos==null)
			return;
		Paint paint = new Paint();
		paint.setTextSize(18);

		paint.setColor(0xFF000000);
		Projection projection = mapView.getProjection();
		Point point = new Point();
		projection.toPixels(pos, point);

		canvas.drawBitmap(bg, point.x - (bg.getWidth() / 2), point.y-bg.getHeight(), paint);
	}

	@Override
	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	public interface OnPositionListener
	{
		void OnPosition(GeoPoint pos);
	}
	
	public void setOnPositionListener(OnPositionListener listener)
	{
		this.listener = listener;
	}
	
	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		Log.d(VKApplication.TAG, "Map up");
		GeoPoint point = mapView.getProjection().fromPixels((int)e.getX(), (int)e.getY());
		this.pos = point;
		if(this.listener!=null)
			listener.OnPosition(pos);
		mapView.invalidate();
		return true;
	}
	
	
}
