package ru.st1ng.vk.model;

public class AudioAttach extends Attachment {

	@Override
	public Type getType() {
		return Type.Audio;
	}

	public String performer;
	public String title;
	public int duration;
	public String url;
}
