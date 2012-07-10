package ru.st1ng.vk.model;

public class PhotoAttach extends Attachment{

	@Override
	public Type getType() {
		return Type.Photo;
	}

	public String photo_src;
	
	public String photo_src_big;
}
