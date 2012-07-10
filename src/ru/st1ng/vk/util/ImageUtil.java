package ru.st1ng.vk.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

public class ImageUtil {

	public static void processRoundedCornerBitmap(String bitmapFile, int pixels) {
		Bitmap bitmap = BitmapFactory.decodeFile(bitmapFile);
		if(bitmap==null)
			return;
        Bitmap output = processRoundedCornerBitmap(bitmap, pixels);
        try {
			output.compress(CompressFormat.PNG, 100, new FileOutputStream(new File(bitmapFile)));
			output.recycle();
		} catch (FileNotFoundException e) {
		}
    }
	
	public static Bitmap processRoundedCornerBitmap(Bitmap bitmap, int pixels)
	{
		if(bitmap==null)
			return null;
        Bitmap output = Bitmap.createBitmap((int)(bitmap.getWidth()), (int)(bitmap
                .getHeight()), Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xFF000000;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final Rect rectOut = new Rect(0, 0, output.getWidth(), output.getHeight());
        final RectF rectFOut = new RectF(rectOut);
        final float roundPx = pixels;

        paint.setAntiAlias(false);
        paint.setFilterBitmap(false);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectFOut, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rectOut, paint);
        bitmap.recycle();
        
        return output;
	}
	
	public static Bitmap decodeBitmapScaledSquare(String bitmapFile, int size)
	{
		Options options = new Options();
		options.inJustDecodeBounds = true;
		Bitmap bitmap = BitmapFactory.decodeFile(bitmapFile, options);
		int sampleSize = calculateInSampleSize(options, size);
		options.inJustDecodeBounds = false;
		options.inSampleSize = sampleSize;
		bitmap = BitmapFactory.decodeFile(bitmapFile,options);
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		if(width>height)
		{
			bitmap = Bitmap.createBitmap(bitmap, (width-height)/2, 0, height, height);
		}
		else if (width<height)
		{
			bitmap = Bitmap.createBitmap(bitmap, 0, (height-width)/2, width, width);
			
		}
		return bitmap;
	}
	
	public static Bitmap processBitmapScaledSquare(Bitmap bitmapFile, int size)
	{
	    if(bitmapFile==null)
	        return null;
		int width = bitmapFile.getWidth();
		int height = bitmapFile.getHeight();
		if(width>height)
		{
			bitmapFile = Bitmap.createBitmap(bitmapFile, (width-height)/2, 0, height, height);
		}
		else if (width<height)
		{
			bitmapFile = Bitmap.createBitmap(bitmapFile, 0, (height-width)/2, width, width);
			
		}
		return bitmapFile;
	}
	private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqSize) {
	    // Raw height and width of image
	    final int size = Math.min(options.outWidth, options.outHeight);
	    int inSampleSize = 1;
	
	    if (size > reqSize) {
	            inSampleSize = Math.round((float)size / (float)reqSize);
	    }
	    return inSampleSize;
	}
	
	public static Bitmap processBitmapDrawVideo(Bitmap bitmap, Bitmap bitmapVideo)
	{
		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(output);
		Paint paint = new Paint();
		canvas.drawBitmap(bitmap,0,0,paint);
		canvas.drawBitmap(bitmapVideo, (bitmap.getWidth()-bitmapVideo.getWidth())/2,(bitmap.getHeight()-bitmapVideo.getHeight())/2,paint);
		bitmap.recycle();
		bitmapVideo.recycle();
		return output;
	}
	
	public static Bitmap processSeveralBitmapsIntoOne(Bitmap... bitmapFiles)
	{
		if(bitmapFiles.length==0)
			return null;
		
		Bitmap bitmap = bitmapFiles[0];
		Bitmap output = Bitmap.createBitmap(bitmapFiles[0].getWidth(), bitmapFiles[0].getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(output);
        final Paint paint = new Paint();
        paint.setColor(0xFFFFFFFF);
		paint.setStrokeWidth(3);
        final int color = 0xFF000000;
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final Rect rectOut = new Rect(0, 0, output.getWidth(), output.getHeight());
        final RectF rectFOut = new RectF(rectOut);
        final float roundPx = 5;
        paint.setAntiAlias(false);
        paint.setFilterBitmap(false);
        canvas.drawARGB(0, 0, 0, 0);
      //  paint.setColor(color);
        canvas.drawRoundRect(rectFOut, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		if(bitmapFiles.length==2)
		{
			Rect in = new Rect(bitmap.getWidth()/4, 0, bitmap.getWidth()/2+bitmap.getWidth()/4, bitmap.getHeight());
			Rect out1 = new Rect(0, 0, bitmap.getWidth()/2, bitmap.getHeight());
			Rect out2 = new Rect(bitmap.getWidth()/2, 0, bitmap.getWidth(), bitmap.getHeight());
			canvas.drawBitmap(bitmapFiles[0], in, out1, paint);
			canvas.drawBitmap(bitmapFiles[1], in, out2, paint);
			canvas.drawLine(bitmap.getWidth()/2, 0, bitmap.getWidth()/2, bitmap.getWidth(), paint);
		}
		else if(bitmapFiles.length==3) {
			Rect in1 = new Rect(bitmap.getWidth()/4, 0, bitmap.getWidth()/2+bitmap.getWidth()/4, bitmap.getHeight());
			Rect in2 = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
			Rect out1 = new Rect(0, 0, bitmap.getWidth()/2, bitmap.getHeight());
			Rect out2 = new Rect(bitmap.getWidth()/2, 0, bitmap.getWidth(), bitmap.getHeight()/2);
			Rect out3 = new Rect(bitmap.getWidth()/2, bitmap.getHeight()/2, bitmap.getWidth(), bitmap.getHeight());
			canvas.drawBitmap(bitmapFiles[0], in1, out1, paint);
			canvas.drawBitmap(bitmapFiles[1], in2, out2, paint);
			canvas.drawBitmap(bitmapFiles[2], in2, out3, paint);
			canvas.drawLine(bitmap.getWidth()/2, 0, bitmap.getWidth()/2, bitmap.getWidth(), paint);
			canvas.drawLine(bitmap.getWidth()/2, bitmap.getHeight()/2, bitmap.getWidth(), bitmap.getHeight()/2, paint);
		} else if(bitmapFiles.length>=4) {
			Rect in = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
			Rect out1 = new Rect(0, 0, bitmap.getWidth()/2, bitmap.getHeight()/2);
			Rect out2 = new Rect(bitmap.getWidth()/2, 0, bitmap.getWidth(), bitmap.getHeight()/2);
			Rect out3 = new Rect(0, bitmap.getHeight()/2, bitmap.getWidth()/2, bitmap.getHeight());
			Rect out4 = new Rect(bitmap.getWidth()/2, bitmap.getHeight()/2, bitmap.getWidth(), bitmap.getHeight());
			canvas.drawBitmap(bitmapFiles[0], in, out1, paint);
			canvas.drawBitmap(bitmapFiles[1], in, out2, paint);
			canvas.drawBitmap(bitmapFiles[2], in, out3, paint);
			canvas.drawBitmap(bitmapFiles[3], in, out4, paint);
			canvas.drawLine(bitmap.getWidth()/2, 0, bitmap.getWidth()/2, bitmap.getWidth(), paint);
			canvas.drawLine(0, bitmap.getHeight()/2, bitmap.getWidth(), bitmap.getHeight()/2, paint);
		}
		return output;
	}
	
	public static Bitmap getScaledBitmap(Bitmap bitmap, float scale)
	{
		Bitmap result = Bitmap.createScaledBitmap(bitmap, (int)(bitmap.getWidth()*scale), (int)(bitmap.getWidth()*scale), false);
		return result;
	}
	
}
