package ru.st1ng.vk.model;

public class DocAttach extends Attachment {

	@Override
	public Type getType() {
		return Type.Doc;
	}

	public String title;
	public long size;
	public String ext;
	public String url;
}
