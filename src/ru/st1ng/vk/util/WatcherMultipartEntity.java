package ru.st1ng.vk.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.http.entity.mime.MultipartEntity;

public class WatcherMultipartEntity extends MultipartEntity {

	private final OutProgressListener listener;
	 
	public WatcherMultipartEntity(final OutProgressListener listener)
	{
		super();
		this.listener = listener;
	}
	
	@Override
	public void writeTo(final OutputStream outstream) throws IOException
	{
		super.writeTo(new CountingOutputStream(outstream, this.listener));
	}
 
	public static interface OutProgressListener
	{
		void transferred(long num);
		void setLength(long lenght);
	}
 
	public static class CountingOutputStream extends FilterOutputStream
	{
 
		private final OutProgressListener listener;
		private long transferred;
 
		public CountingOutputStream(final OutputStream out, final OutProgressListener listener)
		{
			super(out);
			this.listener = listener;
			this.transferred = 0;
		}
  
		public void write(byte[] b, int off, int len) throws IOException
		{
			out.write(b, off, len);
			this.transferred += len;
			this.listener.transferred(this.transferred);
		}
 
		public void write(int b) throws IOException
		{
			out.write(b);
			this.transferred++;
			this.listener.transferred(this.transferred);
		}
	}

}
