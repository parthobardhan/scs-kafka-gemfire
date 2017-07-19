package com.garmin.gemfire.transfer.cachelistener;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.I0Itec.zkclient.ZkClient;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.security.JaasUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.garmin.common.utils.config.ConfigurationData;
import com.garmin.gemfire.transfer.common.TransferConstants;
import com.garmin.gemfire.transfer.keys.LatestTimestampKey;
import com.garmin.gemfire.transfer.util.JSONTypedFormatter;
import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.CacheFactory;
import com.gemstone.gemfire.cache.Declarable;
import com.gemstone.gemfire.cache.EntryEvent;
import com.gemstone.gemfire.cache.Operation;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.util.CacheListenerAdapter;
import com.gemstone.gemfire.internal.cache.EntryEventImpl;
import com.gemstone.gemfire.internal.cache.versions.VersionTag;

import kafka.utils.ZkUtils;

public class KafkaWriter extends CacheListenerAdapter implements Declarable {

	private static final String GEMFIRE_CLUSTER_NAME = "gemfire.cluster.name";
	private static final String LATEST_TIMESTAMP_REGION = "latestTimestamp";
	private static final String KAFKA_BOOTSTRAP_SERVERS = "kafka.bootstrap.servers";
	private static final String KAFKA_ACKNOWLEDGEMENTS = "kafka.acks";
	private static final String KAFKA_NUM_PARTITIONS = "kafka.partition.count";
	private static final String KAFKA_NUM_REPLICAS = "kafka.replica.count";

	private static final String KAFKA_KEY_SERIALIZER = "org.apache.kafka.common.serialization.ByteArraySerializer";
	private static final String KAFKA_VALUE_SERIALIZER = "org.apache.kafka.common.serialization.StringSerializer";

	private static final String ZOOKEEPER_HOSTS = "zookeeper.hosts";
	private static final String ZOOKEEPER_SESSION_TIMEOUT = "zookeeper.session.timeout.ms";
	private static final String ZOOKEEPER_CONNECTION_TIMEOUT = "zookeeper.connection.timeout.ms";
	private static final String ZOOKEEPER_SECURED = "zookeeper.secured";

	// Kafka security
	private static final String KAFKA_SECURITY = "kafka.security";
	private static final String KAFKA_SECURITY_PROTOCOL = "kafka.security.protocol";
	private static final String KAFKA_SASL_MECHANISM = "kafka.sasl.mechanism";
	private static final String KAFKA_CLIENT_SECURITY_LOGIN_CONFIG = "kafka.client.security.login.config";

	private static final String CONST_MONITORING = "MONITORING-";

	private static final Logger LOGGER = LoggerFactory.getLogger(KafkaWriter.class);
	private static final Properties kafkaConfigProperties = new Properties();
	private static ConfigurationData configData = ConfigurationData.getInstance("gemfire-transfer-cachelistener");

	private static Set<String> topicSet = new HashSet<String>();
	private static ZkClient zkClient = null;
	private static ZkUtils zkUtils = null;
	private static Producer kafkaProducer = null;

	private static HashSet eventOperationSupported = new HashSet<String>();

	@Override
	public void afterCreate(EntryEvent event) {
		captureEvent(event);
	}

	@Override
	public void afterUpdate(EntryEvent event) {
		captureEvent(event);
	}

	@Override
	public void afterDestroy(EntryEvent event) {
		captureEvent(event);
	}

	private void captureEvent(EntryEvent event) {
		EntryEventImpl entryEventImpl = (EntryEventImpl) event;
		VersionTag tag = entryEventImpl.getVersionTag();
		long eventTimestamp = tag.getVersionTimeStamp();
		long eventRegionVersion = tag.getRegionVersion();

		if (!verifyEvent(event)) {
			return;
		}

		Cache cache = CacheFactory.getAnyInstance();
		Region latestTimestampRegion = cache.getRegion(LATEST_TIMESTAMP_REGION);
		LatestTimestampKey latestTimestampKey = new LatestTimestampKey(event.getRegion().getName(), event.getKey());
		String topicName = event.getRegion().getName() + "-" + configData.getValue(GEMFIRE_CLUSTER_NAME);
		String region = event.getRegion().getName();
		Object key = event.getKey();
		String keyType = key.getClass().getName();
		Object obj = event.getNewValue();
		String objType = obj != null ? obj.getClass().getName() : "null";

		String jsonTransport;
		try {
			jsonTransport = JSONTypedFormatter.toJsonTransport(key, keyType, obj, objType,
					event.getOperation().toString(), event.getRegion().getName(), eventTimestamp, eventRegionVersion);
			LOGGER.info("Sending EntryEvent to Kafka from region: " + event.getRegion().getName() + " with key: "
					+ event.getKey().toString() + " having operation: " + event.getOperation() + ", timestamp: "
					+ eventTimestamp + ", and updating latestTimestamp region");
			latestTimestampRegion.put(new LatestTimestampKey(event.getRegion().getName(), event.getKey()),
					eventTimestamp);
			sendToKafka(topicName, jsonTransport);
		} catch (JsonProcessingException e) {
			LOGGER.error("Error while parsing JSON object: " + event.getKey().toString() + ", for a region: " + region);
			e.printStackTrace();
		} catch (Exception e) {
			LOGGER.error("Error in Kafka Writer for Key: " + event.getKey().toString() + ", for a region: " + region);
			e.printStackTrace();
		}

	}

	private boolean verifyEvent(EntryEvent event) {
		// To avoid feedback loop between clusters
		if (event.isCallbackArgumentAvailable()) {
			if (event.getCallbackArgument() != null) {
				if (TransferConstants.UPDATE_SOURCE.equals(event.getCallbackArgument().toString())) {
					return false;
				}
			}
		}
		// Some applications puts bogus string with key starts with
		// “MONITORING-“, Ignore such updates
		if (event.getKey().toString().startsWith(CONST_MONITORING)) {
			return false;
		}

		// Process only CRUD operations

		Operation operation = event.getOperation();

		if (!supportedOperations().contains(operation)) {
			LOGGER.warn("Unsupported operation detected: region: " + event.getRegion().getName() + " key: "
					+ event.getKey().toString() + " operation: " + event.getOperation());
			return false;
		}

		return true;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private HashSet supportedOperations() {
		HashSet supportedOperationHashSet = new HashSet<Operation>();
		supportedOperationHashSet.add(Operation.CREATE);
		supportedOperationHashSet.add(Operation.PUTALL_CREATE);
		supportedOperationHashSet.add(Operation.PUTALL_UPDATE);
		supportedOperationHashSet.add(Operation.UPDATE);
		supportedOperationHashSet.add(Operation.REPLACE);
		supportedOperationHashSet.add(Operation.PUT_IF_ABSENT);
		supportedOperationHashSet.add(Operation.DESTROY);
		supportedOperationHashSet.add(Operation.REMOVE);
		supportedOperationHashSet.add(Operation.REMOVEALL_DESTROY);
		supportedOperationHashSet.add(Operation.EXPIRE_DESTROY);
		return supportedOperationHashSet;
	}

	private void sendToKafka(String topicName, String message) {
		try {
			ProducerRecord<String, String> rec = new ProducerRecord<String, String>(topicName, message);
			kafkaProducer.send(rec);
		} catch (Exception e) {
			LOGGER.error("Error while sending data to kafka :" + e.getMessage());
			e.printStackTrace();
			kafkaProducer.close();
		}
	}

	public void init(Properties arg0) {
		// Configure the Producer
		if (configData.getValue(KAFKA_SECURITY) != null) {
			Boolean secure = new Boolean(configData.getValue(KAFKA_SECURITY));
			LOGGER.info("Kafka security :" + secure);
			if (secure) {
				System.setProperty(JaasUtils.JAVA_LOGIN_CONFIG_PARAM,
						configData.getValue(KAFKA_CLIENT_SECURITY_LOGIN_CONFIG));
				kafkaConfigProperties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG,
						configData.getValue(KAFKA_SECURITY_PROTOCOL));
				kafkaConfigProperties.put(SaslConfigs.SASL_MECHANISM, configData.getValue(KAFKA_SASL_MECHANISM));
			}
		}
		kafkaConfigProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
				configData.getValue(KAFKA_BOOTSTRAP_SERVERS));
		kafkaConfigProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KAFKA_KEY_SERIALIZER);
		kafkaConfigProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KAFKA_VALUE_SERIALIZER);
		kafkaConfigProperties.put(ProducerConfig.ACKS_CONFIG, configData.getValue(KAFKA_ACKNOWLEDGEMENTS));
		kafkaProducer = new KafkaProducer<String, String>(kafkaConfigProperties);

	}

}
