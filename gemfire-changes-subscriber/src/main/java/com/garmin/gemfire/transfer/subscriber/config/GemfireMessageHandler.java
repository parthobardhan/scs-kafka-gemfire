package com.garmin.gemfire.transfer.subscriber.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;

import com.garmin.gemfire.transfer.model.TransportRecord;
import com.garmin.gemfire.transfer.util.JSONTypedFormatter;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.client.ClientCache;
import com.gemstone.gemfire.pdx.PdxInstance;

public class GemfireMessageHandler extends AbstractMessageHandler {
	private ClientCache clientCache;
	private Region latestTimestampRegion;

	GemfireMessageHandler(ClientCache clientCache){
		super();
		this.clientCache = clientCache;
		this.latestTimestampRegion = clientCache.getRegion("latestTimestamp");
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
	
/*	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		String payload = (String) message.getPayload();
		TransportRecord transportRec = JSONTypedFormatter.transportRecordFromJson(clientCache, payload);
		Region clientRegion = clientCache.getRegion(transportRec.getRegion());
		Map<String, Object> record = new HashMap<>();
		record.put(transportRec.getKey(), transportRec.getObject());
	}*/
}