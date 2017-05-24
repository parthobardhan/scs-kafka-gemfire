package com.garmin.gemfire.transfer.subscriber.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

public class GemfireSinkHandler {
	private final MessageHandler messageHandler;
	
	GemfireSinkHandler(MessageHandler messageHandler) {
		super();
		this.messageHandler = messageHandler;
	}
	
	public void transform(Message<?> message) {
		messageHandler.handleMessage(message);
	}
}
