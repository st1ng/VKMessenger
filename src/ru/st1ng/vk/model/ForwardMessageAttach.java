package ru.st1ng.vk.model;

public class ForwardMessageAttach extends Attachment {

	@Override
	public Type getType() {
		return Type.Forward;
	}
	
	public String forwardMessages;
}
