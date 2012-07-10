package ru.st1ng.vk.views;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.ImageView;

public class RoundedImageView extends ImageView {

	
	public RoundedImageView(Context context) {
		super(context);
	}
	public RoundedImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	public RoundedImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		super.onDraw(canvas);
	}
}
