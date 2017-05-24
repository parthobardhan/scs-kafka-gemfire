package com.garmin.gemfire.transfer.subscriber.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;

import com.garmin.gemfire.transfer.model.TransportRecord;
import com.garmin.gemfire.transfer.util.JSONTypedFormatter;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.client.ClientCache;

public class GemfireMessageHandler extends AbstractMessageHandler {
	private ClientCache clientCache;

	GemfireMessageHandler(ClientCache clientCache){
		super();
		this.clientCache = clientCache;
	}
	
	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		String payload = (String) message.getPayload();
		TransportRecord transportRec = JSONTypedFormatter.transportRecordFromJson(clientCache, payload);
		Region clientRegion = clientCache.getRegion(transportRec.getRegion());
		Map<String, Object> record = new HashMap<>();
		record.put(transportRec.getKey(), transportRec.getObject());
	}
}