package com.garmin.gemfire.sink;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.MessageHandler;

import com.gemstone.gemfire.cache.Region;

@EnableBinding(Sink.class)
@EnableConfigurationProperties(GemfireSinkProperties.class)
public class GemfireSinkConfig {

	private static Logger logger = LoggerFactory.getLogger(GemfireSinkConfig.class);

	
	@SuppressWarnings("rawtypes")
	@Resource(name = "${gemfire.regionName}")
	Region clientRegion;

	@ServiceActivator(inputChannel = Sink.INPUT)
	@Bean
	public GemfireMessageHandler gemfireMessageHandler() {
		return new GemfireMessageHandler(clientRegion);
	}

	@Bean
	public MessageHandler messageHandler() {
		GemfireMessageHandler messageHandler = new GemfireMessageHandler(clientRegion);
		return messageHandler;
	}
//	@StreamListener(Sink.INPUT)
//	public void loggerSink(GenericMessage<byte[]> payload) {
//		String receivedString = new String(payload.getPayload());
//		logger.info("Received: " + receivedString);
//	}

}
