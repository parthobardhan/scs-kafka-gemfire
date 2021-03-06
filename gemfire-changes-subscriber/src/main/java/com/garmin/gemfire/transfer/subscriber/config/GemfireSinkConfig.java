package com.garmin.gemfire.transfer.subscriber.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.MessageHandler;

import com.gemstone.gemfire.cache.client.ClientCache;

@EnableBinding(Sink.class)
@EnableConfigurationProperties(GemfireSinkProperties.class)
public class GemfireSinkConfig {

	private static Logger logger = LoggerFactory.getLogger(GemfireSinkConfig.class);
	@Autowired
	private GemfireSinkProperties gemfireSinkProperties;

	@Bean
	public ClientCache clientCache() {
		ApplicationContext context = new ClassPathXmlApplicationContext(gemfireSinkProperties.getClientCacheXmlFile());
		ClientCache clientCache = context.getBean(ClientCache.class);
		return clientCache;
	}

	@ServiceActivator(inputChannel = Sink.INPUT)
	@Bean
	public GemfireSinkHandler gemfireSinkHandler() {
		return new GemfireSinkHandler(messageHandler());
	}

	@Bean
	public MessageHandler messageHandler() {
		GemfireMessageHandler messageHandler = new GemfireMessageHandler(clientCache());
		return messageHandler;
	}
}