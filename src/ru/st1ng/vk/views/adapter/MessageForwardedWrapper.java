package ru.st1ng.vk.views.adapter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import ru.st1ng.vk.R;
import ru.st1ng.vk.VKApplication;
import ru.st1ng.vk.data.ImageCache;
import ru.st1ng.vk.model.Attachment;
import ru.st1ng.vk.model.Attachment.Type;
import ru.st1ng.vk.model.AudioAttach;
import ru.st1ng.vk.model.DocAttach;
import ru.st1ng.vk.model.ForwardMessageAttach;
import ru.st1ng.vk.model.GeoAttach;
import ru.st1ng.vk.model.Message;
import ru.st1ng.vk.model.PhotoAttach;
import ru.st1ng.vk.model.VideoAttach;
import ru.st1ng.vk.util.DateFormatUtil;
import ru.st1ng.vk.util.FontsUtil;
import ru.st1ng.vk.util.HttpUtil;
import ru.st1ng.vk.util.ImageUtil;
import ru.st1ng.vk.util.UIUtil;
import ru.st1ng.vk.views.PinchImageView;
import ru.st1ng.vk.views.WebImageView;
import ru.st1ng.vk.views.WebImageView.ImageListener;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.location.Geocoder;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MessageForwardedWrapper {
		private TextView body=null;
		private TextView sender = null;
		private TextView timeRight = null;
		private View row=null;
		private static MediaPlayer mediaPlayer;
		
		private Context context;
        private TextView messageInfo;
        
		public MessageForwardedWrapper(View row, Context context) {
			this.row=row; //The View object that represents a single row
			this.context = context;
		}

		public void populateFrom(Message r) { //Associate to the item components the value from the menu bean
			getBody().setText(r.body);
			getSender().setText(r.user!=null && r.user.first_name!=null ? r.user.first_name : context.getString(R.string.loading));
			getBody().setVisibility(r.body.length()==0 ? View.GONE : View.VISIBLE);
			getCurrentCalendar().setTimeInMillis(r.date*1000);
			if(r.timeString==null)
				r.timeString = DateFormatUtil.getTimeFormat(context).format(getCurrentCalendar().getTime());

			getTimeRight().setVisibility(View.VISIBLE);				
			getTimeRight().setText(r.timeString);
			getSenderImage().setVisibility(View.VISIBLE);
			getSenderImage().setLayoutParams(new LinearLayout.LayoutParams((int)(context.getResources().getDimension(R.dimen.avatar_size_small)*1.5), (int)(context.getResources().getDimension(R.dimen.avatar_size_small)*1.5)));
			if(r.user==null)
			{
						getSenderImage().setImageResource(R.drawable.im_nophoto);
			}
			else
			{
						if(r.user.photo_bitmap!=null)
							getSenderImage().setImageBitmap(r.user.photo_bitmap);
						else
						{
							if(ImageCache.getInstance().isPhotoPresentForUser(r.user))
							{
								r.user.photo_bitmap = ImageCache.getInstance().getPhotoForUser(r.user);
								getSenderImage().setImageBitmap(r.user.photo_bitmap);							
							}
						}
			}
			
            if(r.fwd_messages!=null && r.fwd_messages.size()>0)
            {
                getMessageInfoText().setVisibility(View.VISIBLE);
                getMessageInfoText().setTextSize(FontsUtil.getMessageFontSize(context));
                getMessageInfoText().setText(r.fwd_messages.size() + " " + context.getString(r.fwd_messages.size() == 1 ? R.string.forwarded_message : R.string.forwarded_messages));
                getMessageInfoText().setTextColor(0xFF2d63a0);
                getMessageInfoText().setTypeface(FontsUtil.Helvetica);
            }
            else
            {
                getMessageInfoText().setVisibility(View.GONE);
            }
            
			if(r.attachments!=null)
			{
				int childCount = getAttachesLayout().getChildCount();

				int attaches = r.attachments.size();
				
				int j = 0;
				for(int i =0;i<attaches;i++,j++)
				{
					if(j>=childCount)
					{ 
						LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
						View v = inflater.inflate(R.layout.item_attach, null);
						WebImageView newWeb = (WebImageView) v.findViewById(R.id.photoVideo);
						Button doc = (Button) v.findViewById(R.id.doc);
						View audioControls = v.findViewById(R.id.audioControls);
						
				//		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((int)context.getResources().getDimension(R.dimen.image_size), (int)context.getResources().getDimension(R.dimen.image_size));
				//		params.bottomMargin = 5;
				//		newWeb.setLayoutParams(params);
						newWeb.setOnClickListener(new OnClickListener() {
							
							View inView;
							@Override
							public void onClick(View v) {
								Attachment attach = (Attachment) v.getTag();
								if(inView==null)
									inView = new PinchImageView(context);
								if(attach instanceof PhotoAttach)
								{
									final WebImageView bigImage;
									Dialog d = new Dialog(context,android.R.style.Theme_Translucent_NoTitleBar);
									d.setContentView(R.layout.screen_viewphoto);
									bigImage = (WebImageView) d.findViewById(R.id.photo);
									((AnimationDrawable)bigImage.getDrawable()).start();
									bigImage.setImageListener(new ImageListener() {
										
										@Override
										public void onImageLoaded(WebImageView arg0, Bitmap arg1, String arg2) {
											new PinchImageView(bigImage);											
											bigImage.setImageBitmap(arg1);
										}
									});
									bigImage.setImageFromURL(((PhotoAttach)attach).photo_src_big);
									bigImage.setFocusable(true);
									bigImage.setOnTouchListener(new OnTouchListener() {
										
										@Override
										public boolean onTouch(View v, MotionEvent event) {
											Log.d("VKApp","Touched!");
											return false;
										}
									});
									d.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
									d.show();
//									PopupWindow window = new PopupWindow(inView, LayoutParams.FILL_PARENT, 200, true);
//									((PinchImageView)inView).setImageBitmap(((PhotoAttach)attach).bitmap);
//									window.showAtLocation(v.getRootView(), Gravity.CENTER, 0, 0);
								}
								else if (attach.getType()==Type.Geo)
								{
									GeoAttach geo = (GeoAttach) attach;
									Intent geoIntent = new Intent(Intent.ACTION_VIEW);
									geoIntent.setData(Uri.parse("geo:"+geo.longtitude+","+geo.latitude+"?z=18&q="+geo.longtitude+","+geo.latitude));
									context.startActivity(geoIntent);
								}

							}
						});
						getAttachesLayout().addView(v);
						newWeb.setImageListener(new ImageListener() {
							
							@Override
							public void onImageLoaded(WebImageView arg0, Bitmap arg1, String arg2) {
								arg1 = ImageUtil.processBitmapScaledSquare(arg1, 100);
								if(arg1==null)
								    return;
								arg1 = ImageUtil.processRoundedCornerBitmap(arg1, arg1.getWidth()/10);								
								if(arg1==null)
								    return;
								((Attachment)arg0.getTag()).bitmap = arg1;
								arg0.setImageBitmap(arg1);
							}
						});
						final ImageView playImage = (ImageView) audioControls.findViewById(R.id.playImage);
						playImage.setOnClickListener(new OnClickListener() {
							
							@Override
							public void onClick(View v) {
								final AudioAttach attach = (AudioAttach) v.getTag();
								if(currentUrl!=null && currentUrl.equals(attach.url))
								{
									if(getMediaPlayer().isPlaying())
									{
										getMediaPlayer().pause();
										((ImageView)v).setImageResource(R.drawable.ic_audio_play);

									}
									else
									{
										getMediaPlayer().start();
										((ImageView)v).setImageResource(R.drawable.ic_audio_pause);
										startPlayProgressUpdater();
									}
									return;
								}
								if(currentPlayImage!=null)
									currentPlayImage.setImageResource(R.drawable.ic_audio_play);
								currentPlayImage = (ImageView) v;
								currentUrl = attach.url;

								if(getMediaPlayer().isPlaying())
								{
									getMediaPlayer().seekTo(0);
									getMediaPlayer().stop();
								}
									getMediaPlayer().reset();
								ViewGroup parent = (ViewGroup) v.getParent();
								final SeekBar seekBar = (SeekBar) parent.findViewById(R.id.seek);
								currentSeekBar = seekBar;
								seekBar.setProgress(0);
								seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
									
									@Override
									public void onStopTrackingTouch(SeekBar seekBar) {
										seekBar.setTag(null);
									}
									
									@Override
									public void onStartTrackingTouch(SeekBar seekBar) {
										seekBar.setTag(new Object());
									}
									
									@Override
									public void onProgressChanged(SeekBar seekBar, int progress,
											boolean fromUser) {
										if(progress>seekBar.getSecondaryProgress())
										{
											progress = seekBar.getSecondaryProgress();
											seekBar.setProgress(progress);
										}
										
										if(fromUser && currentUrl.equals(attach.url))
										{
											mediaPlayer.seekTo(progress*mediaPlayer.getDuration()/500);
										}
									}
								});
								getMediaPlayer().setOnSeekCompleteListener(new OnSeekCompleteListener() {
									
									@Override
									public void onSeekComplete(MediaPlayer mp) {
										if(seekBar.getTag()==null)
											seekBar.setProgress(mp.getCurrentPosition()*500/mp.getDuration());											
									}
								});
								getMediaPlayer().setOnBufferingUpdateListener(new OnBufferingUpdateListener() {
									
									@Override
									public void onBufferingUpdate(MediaPlayer mp, int percent) {
										seekBar.setSecondaryProgress(percent*5);
									}
								});
								getMediaPlayer().setOnCompletionListener(new OnCompletionListener() {
									
									@Override
									public void onCompletion(MediaPlayer mp) {
					//					mp.seekTo(0);
					//					playImage.setImageResource(R.drawable.ic_audio_play);
									}
								});
								try {
									getMediaPlayer().setDataSource(attach.url);
									playImage.setImageResource(R.drawable.ic_audio_pause);
									getMediaPlayer().setOnPreparedListener(new OnPreparedListener() {
										
										@Override
										public void onPrepared(MediaPlayer mp) {
											mp.start();
											startPlayProgressUpdater();
										}
									});
									getMediaPlayer().prepareAsync();
								} catch (IllegalArgumentException e) {
								} catch (IllegalStateException e) {
								} catch (IOException e) {
								}
							}
						});
						doc.setOnClickListener(new OnClickListener() {
							
							@Override
							public void onClick(final View v) {
								
								final DocAttach attach = (DocAttach) v.getTag();
								final File docFile = new File(VKApplication.getInstance().getDocsDir(), attach.id + "_" + attach.title);
								if(docFile.exists())
								{
									Intent docIntent = new Intent(Intent.ACTION_VIEW);
									docIntent.setDataAndType(Uri.fromFile(docFile),MimeTypeMap.getSingleton().getMimeTypeFromExtension(attach.ext));
									try{
									context.startActivity(docIntent);
									} catch (Exception e) { UIUtil.showToast(context, context.getString(R.string.app_not_found)); }
								}
								else
								{
									final ProgressDialog dialog = new ProgressDialog(context);
									dialog.setTitle(R.string.downloading);
									dialog.show();
									
									new Thread(new Runnable() {
										
										@Override
										public void run() {
											try {
												HttpUtil.downloadUrlToFile(attach.url, VKApplication.getInstance().getDocsDir(),docFile.getName());
												if(!docFile.exists())
													throw new Exception();
												if(dialog.isShowing())
												{
													dialog.cancel();												
													Intent docIntent = new Intent(Intent.ACTION_VIEW);
													docIntent.setDataAndType(Uri.fromFile(docFile),MimeTypeMap.getSingleton().getMimeTypeFromExtension(attach.ext));
													try{
													context.startActivity(docIntent);
													} catch (Exception e) { UIUtil.showToast(context, context.getString(R.string.app_not_found)); }
												}
											} catch (Exception e) {
												dialog.cancel();
												v.post(new Runnable() {
													
													@Override
													public void run() {
														UIUtil.showToast(context, context.getString(R.string.error_downloading));														
													}
												});
											}
										}
									}).start();
								}
							}
						});
					}
					View v = getAttachesLayout().getChildAt(j);
					WebImageView webImage = (WebImageView) v.findViewById(R.id.photoVideo);
					Button doc = (Button) v.findViewById(R.id.doc);
					View audioControls = v.findViewById(R.id.audioControls);

					Attachment attach = r.attachments.get(i);
					if(attach instanceof PhotoAttach)
					{
						PhotoAttach photo = (PhotoAttach) attach;
						if(photo.photo_src==null)
						{
							webImage.setImageResource(R.anim.ic_spin_animation_blue);
							AnimationDrawable animate = (AnimationDrawable) webImage.getDrawable();
							animate.start();
							continue;
						}
						Attachment tag = (Attachment) webImage.getTag();
						if(tag==null || tag.id!=attach.id)
						{
							if(photo.bitmap==null)
							{
								webImage.setImageResource(R.anim.ic_spin_animation_blue);
								AnimationDrawable animate = (AnimationDrawable) webImage.getDrawable();
								animate.start();
								webImage.setImageFromURL(photo.photo_src);
							}
							else
							{
								webImage.setImageBitmap(photo.bitmap);
							}
							webImage.setTag(attach);
						}
						webImage.setVisibility(View.VISIBLE);
						doc.setVisibility(View.GONE);
						audioControls.setVisibility(View.GONE);
					}						
					else if(attach.getType()==Type.Geo)
					{
						GeoAttach geo = (GeoAttach) attach;
						if(geo.getUrl()==null)
						{
							webImage.setImageResource(R.anim.ic_spin_animation_blue);
							AnimationDrawable animate = (AnimationDrawable) webImage.getDrawable();
							animate.start();
							continue;
						}
						Attachment tag = (Attachment) webImage.getTag();
						if(tag==null || tag.id!=attach.id)
						{
							if(geo.bitmap==null)
							{
								webImage.setImageResource(R.anim.ic_spin_animation_blue);
								AnimationDrawable animate = (AnimationDrawable) webImage.getDrawable();
								animate.start();
								webImage.setImageFromURL(geo.getUrl());
							}
							else
							{
								webImage.setImageBitmap(geo.bitmap);
							}
							webImage.setTag(attach);
						}
						webImage.setVisibility(View.VISIBLE);
						doc.setVisibility(View.GONE);
						audioControls.setVisibility(View.GONE);
					}
					else if(attach instanceof VideoAttach)
					{
						VideoAttach video = (VideoAttach) attach;
						if(video.image==null)
						{
							webImage.setImageResource(R.anim.ic_spin_animation_blue);
							AnimationDrawable animate = (AnimationDrawable) webImage.getDrawable();
							animate.start();
							continue;
						}

						Attachment tag = (Attachment) webImage.getTag();
						if(tag==null || tag.id!=attach.id)
						{
							webImage.setImageFromURL(video.image);
							webImage.setTag(attach);
						}				
						webImage.setVisibility(View.VISIBLE);
						doc.setVisibility(View.GONE);
						audioControls.setVisibility(View.GONE);
					}
					else if(attach instanceof DocAttach)
					{
						DocAttach docAttach = (DocAttach) attach;
						doc.setVisibility(View.VISIBLE);
						webImage.setVisibility(View.GONE);
						audioControls.setVisibility(View.GONE);
						doc.setText(docAttach.title);
						doc.setTag(attach);
					}
					else if(attach.getType()==Type.Audio)
					{
						AudioAttach audio = (AudioAttach) attach;
//						getMediaPlayer().
						doc.setVisibility(View.GONE);
						webImage.setVisibility(View.GONE);
						audioControls.setVisibility(View.VISIBLE);
						((TextView)audioControls.findViewById(R.id.title)).setText(audio.performer);
						((TextView)audioControls.findViewById(R.id.subtitle)).setText(audio.title);
						audioControls.findViewById(R.id.playImage).setTag(attach);
						audioControls.setTag(attach);
					} else if(attach instanceof ForwardMessageAttach)
					{
						ForwardMessageAttach docAttach = (ForwardMessageAttach) attach;
						doc.setVisibility(View.GONE);
						webImage.setVisibility(View.GONE);
						audioControls.setVisibility(View.GONE);
					}
					
				}
				for(int i = attaches;i<childCount;i++)
				{
		//			getAttachesLayout().getChildAt(i).setVisibility(View.GONE);
				}
			}
		}

		
		private TextView getSender() {
			if (sender==null) {
				sender=(TextView)row.findViewById(R.id.messageSender);
			}
			return sender;
		}

		private TextView getBody() {
			if (body==null) {
				body=(TextView)row.findViewById(R.id.messageText);
			}
			return body;
		}
	      private TextView getMessageInfoText() {
	            if (messageInfo==null) {
	                messageInfo=(TextView)row.findViewById(R.id.messageInfoText);
	            }
	            return messageInfo;
	        }
		
		private TextView getTimeRight() {
			if(timeRight==null) {
				timeRight = (TextView) row.findViewById(R.id.timeRight);
			}
			return timeRight;
		}

		Calendar currentCalendar;
		private Calendar getCurrentCalendar()
		{
			if(currentCalendar==null)
				currentCalendar = Calendar.getInstance();
			return currentCalendar;
		}
		
		LinearLayout attachesLayout;
		private LinearLayout getAttachesLayout()
		{
			if(attachesLayout==null)
				attachesLayout=(LinearLayout) row.findViewById(R.id.attachesLayout);
			return attachesLayout;
		}

		ImageView senderImage;
		private ImageView getSenderImage() {
			if(senderImage==null)
				senderImage = (ImageView) row.findViewById(R.id.senderImage);
			return senderImage;
		}
		
		public void startPlayProgressUpdater() {
            currentSeekBar.setProgress(getMediaPlayer().getCurrentPosition()*500/getMediaPlayer().getDuration());

            if (mediaPlayer.isPlaying()) {
                Runnable notification = new Runnable() {
                    public void run() {
                        startPlayProgressUpdater();
                    }
                };
                handler.postDelayed(notification,1000);
            }
        } 
		
		private static final Handler handler = new Handler();
		public static String currentUrl = null;
		public static ImageView currentPlayImage = null;
		public static SeekBar currentSeekBar = null;
		private MediaPlayer getMediaPlayer() {
			if(mediaPlayer==null)
				mediaPlayer = new MediaPlayer();
			return mediaPlayer;
		}
}
