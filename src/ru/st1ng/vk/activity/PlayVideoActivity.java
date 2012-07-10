package ru.st1ng.vk.activity;

import java.util.HashMap;

import ru.st1ng.vk.R;
import ru.st1ng.vk.network.ErrorCode;
import ru.st1ng.vk.network.async.BasicAsyncTask.AsyncCallback;
import ru.st1ng.vk.network.async.VideoGetTask;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Spinner;
import android.widget.VideoView;

public class PlayVideoActivity extends Activity {

	
	public static final String EXTRA_OWNERID = "owner";
	public static final String EXTRA_VIDEOID = "videoid";
	
	HashMap<Integer,String> sizes = null;
	VideoView videoView;
	MediaController videoController;
	Spinner videoSizes;
	ArrayAdapter<Integer> adapter;
	AnimationDrawable progressAnimation;
	ImageView progressImage;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_playvideo);
		videoView = (VideoView) findViewById(R.id.videoView);
		videoController = new MediaController(this);
		videoSizes = (Spinner) findViewById(R.id.sizesSpinner);
		progressImage = (ImageView) findViewById(R.id.progressImage);
		progressAnimation = (AnimationDrawable) progressImage.getDrawable();
		progressAnimation.start();
		
		adapter = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_item);
		videoSizes.setAdapter(adapter);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		videoView.setMediaController(videoController);
		videoController.setAnchorView(videoView);

		videoSizes.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int pos, long arg3) {
			//	videoView.setVisibility(View.INVISIBLE);
				progressImage.setVisibility(View.VISIBLE);
				progressAnimation.start();
				videoView.setVideoURI(Uri.parse(sizes.get(adapter.getItem(pos))));
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				
			}
		});
		videoView.setOnPreparedListener(new OnPreparedListener() {
			
			@Override
			public void onPrepared(MediaPlayer mp) {
			//	videoView.setVisibility(View.VISIBLE);
				progressImage.setVisibility(View.INVISIBLE);
				progressAnimation.stop();
				videoView.start();
			}
		});
		
		new VideoGetTask(new AsyncCallback<HashMap<Integer,String>>() {

			@Override
			public void OnSuccess(HashMap<Integer,String> str) {
				sizes = str;
				if(sizes.containsKey(-1))
				{
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse(sizes.get(-1)));
					startActivity(intent);
					PlayVideoActivity.this.finish();
					return;
				}
				if(sizes.size()>0)
				{
					for(Integer size : sizes.keySet())
					{
						adapter.add(size);
					}
					adapter.notifyDataSetChanged();
					videoView.setVideoURI(Uri.parse(sizes.get(adapter.getItem(0))));
				}
				
			}

			@Override
			public void OnError(ErrorCode errorCode) {
			}
		}).execute(getIntent().getIntExtra(EXTRA_OWNERID, 0), getIntent().getIntExtra(EXTRA_VIDEOID, 0));
	}
}
