package com.garmin.gemfire.sink;

import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;

import com.garmin.gemfire.transfer.model.GemfireChangeEvent;
import com.garmin.gemfire.transfer.model.TransportRecord;
import com.garmin.gemfire.transfer.util.JSONTypedFormatter;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.client.ClientCache;
import com.gemstone.gemfire.cache.client.ClientCacheFactory;
import com.gemstone.gemfire.pdx.PdxInstance;

public class GemfireMessageHandler extends AbstractMessageHandler {
	private final Region clientRegion;
	private final Region latestTimestampRegion;
	private final ClientCache clientCache;
	
	
	public GemfireMessageHandler(Region clientRegion) {
		super();
		this.clientRegion = clientRegion;
		ClientCacheFactory ccf = new ClientCacheFactory();
		this.clientCache=ccf.create();
		this.latestTimestampRegion=clientCache.getRegion("latestTimestamp");
	}
	
	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		String jsonTransport = (String) message.getPayload();
		
		TransportRecord transportRecord=JSONTypedFormatter.transportRecordFromJson(clientCache, jsonTransport);
		String key=transportRecord.getKey();
		Long timestamp=transportRecord.getTimestamp();
		String region=transportRecord.getRegion();
		
		String timestampKey = region + "-" + key;
		
		PdxInstance pi = (PdxInstance) latestTimestampRegion.get(timestampKey);
		Long regionTimestamp = (Long) pi.getField("timestamp");
		if(timestamp > regionTimestamp) {
			// Check timestamp between region and event
			latestTimestampRegion.put(key, timestamp);
			
			//clientRegion.put(key, transportRecord.getObject());
		}
		
	//	clientRegion.put(eventKey, eventObject);
	}
	
	
	/*
	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		GemfireChangeEvent gemfireChangeEvent = (GemfireChangeEvent) message.getPayload();
		Object eventObject = gemfireChangeEvent.getEventObject();
		Object eventKey = gemfireChangeEvent.getEventKey();
		clientRegion.put(eventKey, eventObject);
	}
	*/
}
