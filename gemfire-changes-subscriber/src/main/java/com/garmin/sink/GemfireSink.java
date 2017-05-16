package com.garmin.sink;

import javax.annotation.Resource;
import javax.swing.plaf.synth.Region;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;

@EnableBinding(Sink.class)
public class GemfireSink {

	private static Logger logger = LoggerFactory.getLogger(GemfireSink.class);

	
	@SuppressWarnings("rawtypes")
	@Resource(name = "${gemfire.regionName}")
	Region clientRegion;

	@ServiceActivator(inputChannel = Sink.INPUT)
	@Bean
	public GemfireSinkHandler gemfireSinkHandler() {
		return null;
		//return new GemfireSinkHandler(messageHandler());
	}

	@Bean
	public MessageHandler messageHandler() {
//		GemfireSinkHandler messageHandler = new GemfireSinkHandler(clientRegion);
//		return messageHandler;
		return null;
	}

	
	@StreamListener(Sink.INPUT)
	public void loggerSink(GenericMessage<byte[]> payload) {
		String receivedString = new String(payload.getPayload());
		logger.info("Received: " + receivedString);
	}

}
