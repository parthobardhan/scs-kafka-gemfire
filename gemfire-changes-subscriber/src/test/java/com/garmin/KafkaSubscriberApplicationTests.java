package com.garmin;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.annotation.Bindings;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.garmin.sink.GemfireSink;

//@RunWith(SpringRunner.class)
//@SpringBootTest (classes= {KafkaSubscriberApplication.class})
@WebAppConfiguration
@DirtiesContext
public class KafkaSubscriberApplicationTests {

	@SuppressWarnings("deprecation")
	@Autowired
	@Bindings(GemfireSink.class)
	private Sink sink;
		
/*	@Test
	public void loggerTest() {
		assertNotNull(this.sink.input());
		GenericMessage<String> message = new GenericMessage<String>("this is a test message");
		sink.input().send(message);
		
		assertNotNull(sink.input());
	}
*/
}
