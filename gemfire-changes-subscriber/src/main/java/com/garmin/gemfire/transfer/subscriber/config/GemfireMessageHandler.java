package com.garmin.gemfire.transfer.subscriber.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;

import com.garmin.gemfire.transfer.common.TransferConstants;
import com.garmin.gemfire.transfer.keys.LatestTimestampKey;
import com.garmin.gemfire.transfer.model.TransportRecord;
import com.garmin.gemfire.transfer.util.JSONTypedFormatter;
import com.gemstone.gemfire.cache.Operation;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.client.ClientCache;

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
		Object key = transportRecord.getKey();
		long messageLatestTimestamp = transportRecord.getTimestamp();
		String region = transportRecord.getRegion();
		String operation = transportRecord.getOperation();

		logger.debug("Received message with " + operation + " operation on Region :" + region + " for key: " + key
				+ " with timestamp: " + messageLatestTimestamp);

		LatestTimestampKey latestTimestampKey = new LatestTimestampKey(region, key);

		boolean shouldProcessMessage = false;
		if (latestTimestampRegion.containsKeyOnServer(latestTimestampKey)) {
			long regionLatestTimestamp = (long) latestTimestampRegion.get(latestTimestampKey);

			if (messageLatestTimestamp > regionLatestTimestamp) {
				logger.debug("Message for Region : " + region + " with operation: " + operation + " for key: " + key
						+ " with timestamp: " + messageLatestTimestamp
						+ "is more recent than object on region with timestamp: " + regionLatestTimestamp
						+ ", so should be processing Message");
				latestTimestampRegion.put(latestTimestampKey, messageLatestTimestamp);
				shouldProcessMessage = true;
			} else {
				logger.info("The message with:" + key + " for region :" + region + " with message timestamp: "
						+ messageLatestTimestamp + " is earlier than the region LatestTimestamp : "
						+ regionLatestTimestamp + ", so should not be processing message ");
				shouldProcessMessage = false;
			}
		} else {
			logger.debug("key " + latestTimestampKey.toString()
					+ "does not exist on server, so should be processing Message");
			latestTimestampRegion.put(latestTimestampKey, messageLatestTimestamp);
			shouldProcessMessage = true;
		}
		if (shouldProcessMessage) {
			logger.debug("processing message with key: " + latestTimestampKey.toString() + " operation: "
					+ transportRecord.getOperation() + " messageLatestTimestamp: " + messageLatestTimestamp);
			processMessage(latestTimestampKey, transportRecord.getObject(), transportRecord.getOperation(),
					messageLatestTimestamp);
		}
	}

	private void processMessage(LatestTimestampKey latestTimestampKey, Object value, String operation, long timestamp)
			throws InterruptedException {
		Region clientRegion = clientCache.getRegion(latestTimestampKey.getRegion());

		if (Operation.CREATE.toString().equals(operation) || Operation.PUTALL_CREATE.toString().equals(operation)
				|| Operation.PUTALL_UPDATE.toString().equals(operation) || Operation.UPDATE.toString().equals(operation)
				|| Operation.REPLACE.toString().equals(operation)) {
			logger.info("performing " + operation + " operation on Region :" + latestTimestampKey.getRegion()
					+ " for key " + latestTimestampKey.getKey() + " with timestamp " + timestamp);
			clientRegion.put(latestTimestampKey.getKey(), value, TransferConstants.UPDATE_SOURCE);
		} else if (Operation.PUT_IF_ABSENT.toString().equals(operation)
				&& !clientRegion.containsKey(latestTimestampKey.getKey())) {
			logger.info("performing " + operation + " operation on Region :" + latestTimestampKey.getRegion()
					+ " for key " + latestTimestampKey.getKey() + " with timestamp " + timestamp);
			clientRegion.put(latestTimestampKey.getKey(), value, TransferConstants.UPDATE_SOURCE);
		} else if (Operation.REMOVEALL_DESTROY.toString().equals(operation)
				|| Operation.DESTROY.toString().equals(operation) || (Operation.REMOVE.toString().equals(operation))) {
			logger.info("performing " + operation + " operation on Region :" + latestTimestampKey.getRegion()
					+ " for key " + latestTimestampKey.getKey() + " with timestamp " + timestamp);
			clientRegion.destroy(latestTimestampKey.getKey(), TransferConstants.UPDATE_SOURCE);
		} else if (Operation.EXPIRE_DESTROY.toString().equals(operation)) {
			logger.info("performing " + operation + " operation on Region :" + latestTimestampKey.getRegion()
					+ " for key " + latestTimestampKey.getKey() + " with timestamp " + timestamp);
			clientRegion.destroy(latestTimestampKey.getKey(), TransferConstants.UPDATE_SOURCE);
		} else
			logger.error("The operation " + operation
					+ " is not implemented and gemfire regions are not updated. Please implement this scenario for proper bi-directional flow");
	}
}