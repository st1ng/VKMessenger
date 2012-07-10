package ru.st1ng.vk.activity;

import ru.st1ng.vk.R;
import android.app.Activity;
import android.os.Bundle;

public class EmptyActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_empty);
	}
}
