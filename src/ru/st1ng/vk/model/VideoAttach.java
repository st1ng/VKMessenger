package ru.st1ng.vk.model;

public class VideoAttach extends Attachment {

	@Override
	public Type getType() {
		return Type.Video;
	}
	
	public int duration;
	
	public String title;
	
	public String image;
}
