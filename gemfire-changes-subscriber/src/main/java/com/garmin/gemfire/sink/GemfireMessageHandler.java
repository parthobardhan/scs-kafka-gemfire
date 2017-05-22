package com.garmin.gemfire.sink;

import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;

import com.garmin.gemfire.transfer.model.GemfireChangeEvent;
import com.gemstone.gemfire.cache.Region;

public class GemfireMessageHandler extends AbstractMessageHandler {
	private final Region clientRegion;
	
	public GemfireMessageHandler(Region clientRegion) {
		super();
		this.clientRegion = clientRegion;
	}
	
	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		GemfireChangeEvent gemfireChangeEvent = (GemfireChangeEvent) message.getPayload();
		Object eventObject = gemfireChangeEvent.getEventObject();
		Object eventKey = gemfireChangeEvent.getEventKey();
		clientRegion.put(eventKey, eventObject);
	}
}
