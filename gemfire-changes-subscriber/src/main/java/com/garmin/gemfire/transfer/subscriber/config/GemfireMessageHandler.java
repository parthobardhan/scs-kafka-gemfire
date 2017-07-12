package com.garmin.gemfire.transfer.subscriber.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;

import com.garmin.gemfire.transfer.common.TransferConstants;
import com.garmin.gemfire.transfer.keys.LatestTimestampKey;
import com.garmin.gemfire.transfer.model.LatestTimestamp;
import com.garmin.gemfire.transfer.model.TransportRecord;
import com.garmin.gemfire.transfer.util.JSONTypedFormatter;
import com.gemstone.gemfire.cache.CacheTransactionManager;
import com.gemstone.gemfire.cache.CommitConflictException;
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
		Object key = transportRecord.getKey();
		long messageLatestTimestamp = transportRecord.getTimestamp();
		long messageRegionVersion = transportRecord.getRegionVersion();
		String region = transportRecord.getRegion();
		String operation = transportRecord.getOperation();

		logger.debug("Received message with " + operation + " operation on Region :" + region + " for key: " + key
				+ " with timestamp: " + messageLatestTimestamp + " and messageRegionVersion: " + messageRegionVersion);

		LatestTimestampKey latestTimestampKey = new LatestTimestampKey(region, key);

		CacheTransactionManager txmgr = clientCache.getCacheTransactionManager();
		boolean commitConflict = false;
		int i = 0;
		boolean shouldProcessMessage = false;
		do {
			try {
				txmgr.begin();
				if (latestTimestampRegion.containsKeyOnServer(latestTimestampKey)) {
					Object latestTimestampObject = latestTimestampRegion.get(latestTimestampKey);
					PdxInstance latestTimestampPdxInstace = (PdxInstance) latestTimestampObject;
					long regionRegionVersion = (Long) latestTimestampPdxInstace.getField("regionVersion");
					long regionLatestTimestamp = (Long) latestTimestampPdxInstace.getField("latestTimestamp");

					logger.debug("Message with " + operation + " operation on Region :" + region + "for key " + key
							+ " with timestamp on message: " + messageLatestTimestamp
							+ " while timestamp on region is: " + regionLatestTimestamp + " regionVersion in message: "
							+ messageRegionVersion + " while regionVersion on region is: " + regionRegionVersion);
					if (messageLatestTimestamp > regionLatestTimestamp && messageRegionVersion > regionRegionVersion) {
						logger.debug("Incoming message is more recent, so should be processing Message");
						shouldProcessMessage = true;
					} else {
						logger.info("The message with:" + key + " for region :" + region + " with message timestamp: "
								+ messageLatestTimestamp + " and message regionVersion: " + messageRegionVersion
								+ " is earlier than the region LatestTimestamp : " + regionLatestTimestamp
								+ " and region RegionVersion: " + regionRegionVersion
								+ ", hence not updating to region");
						shouldProcessMessage = false;
					}
				} else {
					logger.debug("key " + latestTimestampKey.toString()
							+ "does not exist on server, so should be processing Message");
					shouldProcessMessage = true;
				}
				if (shouldProcessMessage) {
					LatestTimestamp latestTimestamp = new LatestTimestamp(messageLatestTimestamp, messageRegionVersion);
					latestTimestampRegion.put(latestTimestampKey, latestTimestamp);
				}
				txmgr.commit();
				if (logger.isDebugEnabled()) {
					PdxInstance latestTimestampPdxInstace = (PdxInstance) latestTimestampRegion
							.get(latestTimestampKey);
					long regionRegionVersionAfterPut = (Long) latestTimestampPdxInstace.getField("regionVersion");
					long regionLatestTimestampAfterPut = (Long) latestTimestampPdxInstace
							.getField("latestTimestamp");
					logger.debug("After put, for key: " + latestTimestampKey.toString()
							+ " value in latestTimestamp region: latestTimestamp: " + regionLatestTimestampAfterPut
							+ " regionVersion: " + regionRegionVersionAfterPut);
				}
				commitConflict = false;
			} catch (CommitConflictException conflict) {
				commitConflict = true;
				i++;
				logger.debug("Retry " + i + " to commit for key: " + latestTimestampKey.getKey());
			} catch (Exception e) {
				if (txmgr.exists())
					txmgr.rollback();
				logger.error("Other Exception in Subscriber transaction", e);
				return;
			}
		} while (commitConflict && i < 4);

		if (commitConflict) {
			if (txmgr.exists())
				txmgr.rollback();
			logger.error(
					"4 attempts to write to latestTimestamp region for key " + latestTimestampKey.getKey() + " region: "
							+ latestTimestampKey.getRegion() + " and timestamp: " + messageLatestTimestamp + " failed");
			return;
		}

		if (shouldProcessMessage) {
			logger.debug("processing message with key: " + latestTimestampKey.toString() + " operation: "
					+ transportRecord.getOperation() + " messageLatestTimestamp: " + messageLatestTimestamp
					+ " messageRegionVersion" + messageRegionVersion);
			processMessage(latestTimestampKey, transportRecord.getObject(), transportRecord.getOperation(),
					messageLatestTimestamp, messageRegionVersion);
		}
	}

	private void processMessage(LatestTimestampKey latestTimestampKey, Object value, String operation, long timestamp,
			long regionVersion) throws InterruptedException {
		logger.debug("Updated timestamp on latestTimestamp region for region " + latestTimestampKey.getRegion()
				+ " key: " + latestTimestampKey.getKey() + " to: " + timestamp);

		Region clientRegion = clientCache.getRegion(latestTimestampKey.getRegion());

		if (Operation.CREATE.toString().equals(operation) || Operation.PUTALL_CREATE.toString().equals(operation)
				|| Operation.PUTALL_UPDATE.toString().equals(operation) || Operation.UPDATE.toString().equals(operation)
				|| Operation.REPLACE.toString().equals(operation)) {
			logger.info("performing " + operation + " operation on Region :" + latestTimestampKey.getRegion()
					+ "for key " + latestTimestampKey.getKey() + " with timestamp " + timestamp);
			clientRegion.put(latestTimestampKey.getKey(), value, TransferConstants.UPDATE_SOURCE);
		} else if (Operation.PUT_IF_ABSENT.toString().equals(operation)
				&& !clientRegion.containsKey(latestTimestampKey.getKey())) {
			logger.info("performing " + operation + " operation on Region :" + latestTimestampKey.getRegion()
					+ "for key " + latestTimestampKey.getKey() + " with timestamp " + timestamp);
			clientRegion.put(latestTimestampKey.getKey(), value, TransferConstants.UPDATE_SOURCE);
		} else if (Operation.REMOVEALL_DESTROY.toString().equals(operation)
				|| Operation.DESTROY.toString().equals(operation) || (Operation.REMOVE.toString().equals(operation))) {
			logger.info("performing " + operation + " operation on Region :" + latestTimestampKey.getRegion()
					+ "for key " + latestTimestampKey.getKey() + " with timestamp " + timestamp);
			clientRegion.destroy(latestTimestampKey.getKey(), TransferConstants.UPDATE_SOURCE);
		} else if (Operation.EXPIRE_DESTROY.toString().equals(operation)) {
			logger.info("performing " + operation + " operation on Region :" + latestTimestampKey.getRegion()
					+ "for key " + latestTimestampKey.getKey() + " with timestamp " + timestamp);
			clientRegion.destroy(latestTimestampKey.getKey(), TransferConstants.UPDATE_SOURCE);
		} else
			logger.error("The operation " + operation
					+ " is not implemented and gemfire regions are not updated. Please implement this scenario for proper bi-directional flow");
	}
}