package ru.st1ng.vk.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

public class PinchImageView extends ImageView implements OnTouchListener  {
	
	public static final int GROW = 0;
	public static final int SHRINK = 1;
	
	public static final int TOUCH_INTERVAL = 0;
	
	public static final float MIN_SCALE = 1f;
	public static final float MAX_SCALE = 4f;
	public static final float ZOOM = 1.5f;
	
	ImageView im = null;
	Matrix mBaseMatrix = new Matrix(), 
		mSuppMatrix = new Matrix(), 
		mResultMatrix = new Matrix();
	Bitmap mBitmap = null;
	
	float xCur, yCur, 
		xPre, yPre,
		xSec, ySec,
		distDelta,
		distCur, distPre = -1;
	int mWidth, mHeight, mTouchSlop;
	long mLastGestureTime;
	
	public PinchImageView(Context context,AttributeSet attr) {
         super(context,attr);
         _init();
    }
	
	public PinchImageView(Context context) {
        super(context);        
        _init();
    }
	
	public PinchImageView(ImageView im) {
		super(im.getContext());
		_init();
		this.im = im;
		this.im.setScaleType(ScaleType.MATRIX);
		this.im.setOnTouchListener(this);
	}
	
	public float getScale() {
		return getScale(mSuppMatrix);
	}
	
	public float getScale(Matrix matrix) {
		float[] values = new float[9];
		 matrix.getValues(values);
		return values[Matrix.MSCALE_X];
	}
	
	public void setImageBitmap(Bitmap bm) {
		super.setImageBitmap(bm);
		Drawable d = getDrawable();
		if (d != null) {
			d.setDither(true);
		}
		mBitmap = bm;
		center(true, true);
	}
	
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		mWidth = right - left;
		mHeight = bottom - top;
		
		if (mBitmap != null) {
			getProperBaseMatrix(mBaseMatrix);
			setImageMatrix(getImageViewMatrix());
		}
	}
	
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction() & MotionEvent.ACTION_MASK, 
			p_count = event.getPointerCount();
	
	    switch (action) {
	    case MotionEvent.ACTION_MOVE:
	    	
	    	// point 1 coords
    		xCur = event.getX(0);
    		yCur = event.getY(0);
	    	if (p_count > 1) {
	    		// point 2 coords
	    		xSec = event.getX(1);
	    		ySec = event.getY(1);
	    		
	    		// distance between
	    		distCur = (float) Math.sqrt(Math.pow(xSec - xCur, 2) + Math.pow(ySec - yCur, 2));
	    		distDelta = distPre > -1 ? distCur - distPre : 0;
	    		
				float scale = getScale();
		    	long now = android.os.SystemClock.uptimeMillis();
		    	if (Math.abs(distDelta) > mTouchSlop) {
		    		mLastGestureTime = 0;
		    		
		    		//ScaleAnimation scale = null;
	    			int mode = distDelta > 0 ? GROW : (distCur == distPre ? 2 : SHRINK);
		    		switch (mode) {
		    		case GROW: // grow
		    			zoomIn(scale);
		    		break;
		    		case SHRINK: // shrink
		    			zoomOut(scale);
		    		break;
		    		}
		    		
		    		mLastGestureTime = now;
		    		xPre = xCur;
			    	yPre = yCur;
					distPre = distCur;
		    		return true;
	    		}
	    	}
	    	
	    	xPre = xCur;
	    	yPre = yCur;
			distPre = distCur;
	    break;
	    case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_UP:
    		distPre = -1;
    		mLastGestureTime = android.os.SystemClock.uptimeMillis();
	    break;
	    }
	    
	    return false;
	}
	
	private void _init() {
		im = this;
		mTouchSlop = ViewConfiguration.getTouchSlop();
		im.setScaleType(ScaleType.MATRIX);
	}

	public boolean onTouch(View v, MotionEvent event) {
		return this.onTouchEvent(event);
	}
	
	public void zoomMax() {
		zoomTo(MAX_SCALE);
	}
	
	public void zoomMin() {
		zoomTo(MIN_SCALE);
	}
	
	public synchronized void postTranslate(float dx, float dy) {
		mSuppMatrix.postTranslate(dx, dy);
	}
	
	public void center(boolean horizontal, boolean vertical) {
		if (mBitmap == null) {
			return;
		}

		Matrix m = getImageViewMatrix();
		RectF rect = new RectF(0, 0,
				mBitmap.getWidth(),
				mBitmap.getHeight());

		m.mapRect(rect);

		float height = rect.height(), 
			width  = rect.width(), 
			deltaX = 0, 
			deltaY = 0;

		if (vertical) {
			int viewHeight = getHeight();
			if (height < viewHeight) {
				deltaY = (viewHeight - height) / 2f - rect.top;
			} 
			else if (rect.top > 0) {
				deltaY = -rect.top;
			} 
			else if (rect.bottom < viewHeight) {
				deltaY = getHeight() - rect.bottom;
			}

		}

		if (horizontal) {
			int viewWidth = getWidth();
			if (width < viewWidth) {
				deltaX = (viewWidth - width) / 2 - rect.left;

			} 
			else if (rect.left > 0) {
				deltaX = -rect.left;
			} 
			else if (rect.right < viewWidth) {
				deltaX = viewWidth - rect.right;
			}
		}

		postTranslate(deltaX, deltaY);
		setImageMatrix(getImageViewMatrix());
	}
	
	protected Matrix getImageViewMatrix() {
		mResultMatrix.set(mBaseMatrix);
		mResultMatrix.postConcat(mSuppMatrix);
		return mResultMatrix;
	}
	
	protected void zoomTo(float scale) {
		mSuppMatrix.setScale(scale, scale, getWidth() / 2f, getHeight() / 2f);
		setImageMatrix(getImageViewMatrix());
		center(true, true);
	}
	
	protected void zoomIn(float scale) {
		if (scale > MAX_SCALE) return;
		
		mSuppMatrix.postScale(ZOOM, ZOOM, getWidth() / 2f, getHeight() / 2f);
		setImageMatrix(getImageViewMatrix());
	}
	
	protected void zoomOut(float scale) {
		if (scale < MIN_SCALE) return;
		
		float cx = getWidth() / 2f,
			cy = getHeight() / 2f,
			diff = 1f / ZOOM;
		
		Matrix tmp = new Matrix(mSuppMatrix);
		tmp.postScale(diff, diff, cx, cy);
		
		if (getScale(tmp) < MIN_SCALE) {
			mSuppMatrix.setScale(MIN_SCALE, MIN_SCALE, cx, cy);
		}
		else {
			mSuppMatrix.postScale(diff, diff, cx, cy);
		}
		
		setImageMatrix(getImageViewMatrix());
		center(true, true);
	}
	
	private void getProperBaseMatrix(Matrix matrix) {
		float viewWidth = getWidth(),
			viewHeight = getHeight();
		
		float w = mBitmap.getWidth(), 
			h = mBitmap.getHeight();
		
		matrix.reset();
		
		float widthScale = Math.min(viewWidth / w, MAX_SCALE),
			heightScale = Math.min(viewHeight / h, MAX_SCALE),
			scale = Math.min(widthScale, heightScale);
		
		Matrix bitmapMatrix = new Matrix();
		bitmapMatrix.preTranslate(-(mBitmap.getWidth() >> 1), -(mBitmap.getHeight() >> 1));
		bitmapMatrix.postTranslate(getWidth() / 2, getHeight() / 2);
		
		matrix.postConcat(bitmapMatrix);
		matrix.postScale(scale, scale);
		matrix.postTranslate((viewWidth  - w * scale) / 2F, (viewHeight - h * scale) / 2F);
	}

}
