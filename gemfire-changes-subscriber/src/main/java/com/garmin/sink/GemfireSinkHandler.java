package com.garmin.sink;

import org.springframework.messaging.Message;

import com.garmin.gemfire.transfer.model.GemfireChangeEvent;
import com.gemstone.gemfire.cache.Region;

public class GemfireSinkHandler {
	//private final Region clientRegion;
	public void handleMessage(Message<?> message){
		GemfireChangeEvent gemfireChangeEvent = (GemfireChangeEvent) message.getPayload();
		Object eventObject = gemfireChangeEvent.getEventObject();
		Object eventKey = gemfireChangeEvent.getEventKey();
		
	}

}
