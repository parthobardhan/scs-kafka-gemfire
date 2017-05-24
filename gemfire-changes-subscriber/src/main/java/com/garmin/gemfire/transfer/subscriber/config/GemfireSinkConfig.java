package com.garmin.gemfire.transfer.subscriber.config;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.MessageHandler;

import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.client.ClientCache;

@EnableBinding(Sink.class)
@EnableConfigurationProperties(GemfireSinkProperties.class)
public class GemfireSinkConfig {

	private static Logger logger = LoggerFactory.getLogger(GemfireSinkConfig.class);

	//
	// @SuppressWarnings("rawtypes")
	// @Resource(name = "${gemfire.regionName}")
	// Region clientRegion;

	@Autowired
	ClientCache clientCache;

	@ServiceActivator(inputChannel = Sink.INPUT)
	@Bean
	public GemfireSinkHandler gemfireSinkHandler() {
		return new GemfireSinkHandler(messageHandler());
	}

	@Bean
	public MessageHandler messageHandler() {
		GemfireMessageHandler messageHandler = new GemfireMessageHandler(clientCache);
		return messageHandler;
	}
}
