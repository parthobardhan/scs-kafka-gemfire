package com.garmin.gemfire.transfer.subscriber.config;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;

import com.garmin.gemfire.transfer.common.TransferConstants;
import com.garmin.gemfire.transfer.model.TransportRecord;
import com.garmin.gemfire.transfer.util.JSONTypedFormatter;
import com.gemstone.gemfire.cache.Operation;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.client.ClientCache;
import com.gemstone.gemfire.pdx.PdxInstance;

public class GemfireMessageHandler extends AbstractMessageHandler {

	private static Logger logger = LoggerFactory.getLogger(GemfireMessageHandler.class);
	private ClientCache clientCache;
	private Region latestTimestampRegion;

	GemfireMessageHandler(ClientCache clientCache) {
		super();
		this.clientCache = clientCache;
		this.latestTimestampRegion = clientCache.getRegion("latestTimestamp");
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		String jsonTransport = new String((byte[]) message.getPayload());
		TransportRecord transportRecord = JSONTypedFormatter.transportRecordFromJson(clientCache, jsonTransport);
		String key = transportRecord.getKey();
		Long timestamp = transportRecord.getTimestamp();
		String region = transportRecord.getRegion();

		String timestampKey = region + "-" + key;
		PdxInstance pi = (PdxInstance) latestTimestampRegion.get(timestampKey);
		Long regionTimestamp = -1L;
		// first time, the latesttimestamp region object is null
		if (pi != null) {
			regionTimestamp = (Long) pi.getField("timestamp");
		}
		if (timestamp > regionTimestamp) {
			// Check timestamp between region and event
			latestTimestampRegion.put(key, timestamp);
			Region clientRegion = clientCache.getRegion(region);

			// put and putAll
			String operation = transportRecord.getOperation();
			logger.debug("Received messaage with " + operation + " operation on Region :" + region + "for key " + key);
			if (Operation.CREATE.toString().equals(operation) || Operation.PUTALL_CREATE.toString().equals(operation)
					|| Operation.PUTALL_UPDATE.toString().equals(operation)
					|| Operation.UPDATE.toString().equals(operation)
					|| Operation.REPLACE.toString().equals(operation)) {
				logger.debug("performing " + operation + " operation on Region :" + region + "for key " + key);
				clientRegion.put(key, transportRecord.getObject(), TransferConstants.UPDATE_SOURCE);
			} else if (Operation.PUT_IF_ABSENT.toString().equals(operation) && !clientRegion.containsKey(key)) {
				logger.debug("performing " + operation + " operation on Region :" + region + "for key " + key);
				clientRegion.put(key, transportRecord.getObject(), TransferConstants.UPDATE_SOURCE);
			} else if (Operation.REMOVEALL_DESTROY.toString().equals(operation)
					|| Operation.DESTROY.toString().equals(operation)
					|| (Operation.REMOVE.toString().equals(operation))) {
				logger.debug("performing " + operation + " operation on Region :" + region + "for key " + key);
				clientRegion.destroy(key, TransferConstants.UPDATE_SOURCE);
			}
			else if (Operation.EXPIRE_DESTROY.toString().equals(operation)){
				logger.debug("performing " + operation + " operation on Region :" + region + "for key " + key);
				clientRegion.destroy(key, TransferConstants.UPDATE_SOURCE);
			}
		} else
			logger.warn("The object :" + key + " for a region :" + region + " is older, hence not updating to region.");

	}

}