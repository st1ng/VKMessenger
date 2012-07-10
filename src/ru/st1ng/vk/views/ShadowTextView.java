package ru.st1ng.vk.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;
import android.widget.Toast;

public class ShadowTextView extends TextView {

	public ShadowTextView(Context context) {
		super(context);
	}
	public ShadowTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	public ShadowTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	
	protected void onDraw(android.graphics.Canvas canvas) {
		super.onDraw(canvas);
	};
}
