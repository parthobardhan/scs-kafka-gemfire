package com.garmin.gemfire.transfer.cachelistener;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.garmin.common.utils.config.ConfigurationData;
import com.garmin.gemfire.transfer.common.TransferConstants;
import com.garmin.gemfire.transfer.util.JSONTypedFormatter;
import com.gemstone.gemfire.cache.Declarable;
import com.gemstone.gemfire.cache.EntryEvent;
import com.gemstone.gemfire.cache.util.CacheListenerAdapter;
import com.gemstone.gemfire.internal.cache.EntryEventImpl;
import com.gemstone.gemfire.internal.cache.versions.VersionTag;

import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.common.TopicExistsException;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;

public class KafkaWriter extends CacheListenerAdapter implements Declarable {

	private static final String GEMFIRE_CLUSTER_NAME = "gemfire.cluster.name";

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

	private static final String CONST_MONITORING = "MONITORING-";

	private static final Logger LOGGER = LoggerFactory.getLogger(KafkaWriter.class);
	private static final Properties kafkaConfigProperties = new Properties();
	private static ConfigurationData configData = ConfigurationData.getInstance("gemfire-transfer-cachelistener");

	private static Set<String> topicSet = new HashSet<String>();
	private static ZkClient zkClient = null;
	private static ZkUtils zkUtils = null;
	private static Producer kafkaProducer = null;

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
		LOGGER.info("EntryEvent details: key: " + event.getKey() + " operation: " + event.getOperation() + "timestamp: "
				+ eventTimestamp);
		// To avoid feedback loop between clusters
		if (event.isCallbackArgumentAvailable()) {
			if (event.getCallbackArgument() != null) {
				if (TransferConstants.UPDATE_SOURCE.equals(event.getCallbackArgument().toString()))
					return;
			}
		}
		// Some applications puts bogus string with key starts with
		// “MONITORING-“, Ignore such updates
		if (event.getKey().toString().startsWith(CONST_MONITORING))
			return;

		String topicName = event.getRegion().getName() + "-" + configData.getValue(GEMFIRE_CLUSTER_NAME);
		if (!topicSet.contains(topicName)) {
			try {
				AdminUtils.createTopic(zkUtils, topicName, Integer.parseInt(configData.getValue(KAFKA_NUM_PARTITIONS)),
						Integer.parseInt(configData.getValue(KAFKA_NUM_REPLICAS)), new Properties(),
						RackAwareMode.Safe$.MODULE$);
				topicSet.add(topicName);
				LOGGER.info("Created topic: " + topicName);
			} catch (TopicExistsException e) {
				LOGGER.info("Topic " + topicName + " already exists.");
				topicSet.add(topicName);
			} catch (Exception ex) {
				LOGGER.error("Error while creating topic :" + topicName);
				ex.printStackTrace();
			}
		}

		String region = event.getRegion().getName();

		String jsonTransport;
		try {
			jsonTransport = JSONTypedFormatter.toJsonTransport(event.getKey().toString(), event.getNewValue(),
					event.getOperation().toString(), event.getRegion().getName(), eventTimestamp);
			sendToKafka(topicName, jsonTransport);
		} catch (JsonProcessingException e) {
			LOGGER.error("Error while parsing JSON object: " + event.getKey().toString() + ", for a region: " + region);
			e.printStackTrace();
		}

	}

	private void sendToKafka(String topicName, String message) {
		// Externalize the properties with bookstrap, acks, block on buffer,
		// retries etc.
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
		kafkaConfigProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
				configData.getValue(KAFKA_BOOTSTRAP_SERVERS));
		kafkaConfigProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KAFKA_KEY_SERIALIZER);
		kafkaConfigProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KAFKA_VALUE_SERIALIZER);
		kafkaConfigProperties.put(ProducerConfig.ACKS_CONFIG, configData.getValue(KAFKA_ACKNOWLEDGEMENTS));

		zkClient = new ZkClient(configData.getValue(ZOOKEEPER_HOSTS),
				Integer.parseInt(configData.getValue(ZOOKEEPER_SESSION_TIMEOUT)),
				Integer.parseInt(configData.getValue(ZOOKEEPER_CONNECTION_TIMEOUT)), ZKStringSerializer$.MODULE$);
		zkUtils = new ZkUtils(zkClient, new ZkConnection(configData.getValue(ZOOKEEPER_HOSTS)),
				Boolean.parseBoolean(configData.getValue(ZOOKEEPER_SECURED)));
		kafkaProducer = new KafkaProducer<String, String>(kafkaConfigProperties);

	}

}
