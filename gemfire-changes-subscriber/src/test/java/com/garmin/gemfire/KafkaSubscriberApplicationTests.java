package com.garmin.gemfire;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.cloud.stream.test.binder.MessageCollector;

import com.garmin.gemfire.KafkaSubscriberApplication;
import com.garmin.gemfire.transfer.model.GemfireChangeEvent;
import com.gemstone.gemfire.cache.Region;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = { KafkaSubscriberApplication.class })
@DirtiesContext
public abstract class KafkaSubscriberApplicationTests {

	@Autowired
	protected Sink sink;

	@Autowired
	protected MessageCollector messageCollector;
	
	@Autowired
	@Qualifier("customer")
	protected Region customerRegion;

	@SpringBootTest("gemfire.regionName=customer")
	public static class CustomerTests extends KafkaSubscriberApplicationTests {

		@Test
		public void testAddCustomer() throws InterruptedException {
			customerRegion.clear();
			Object keyObject = new String("Key");
			Object valueObject = new String("Value");
			Date changeDate = new Date();
			GemfireChangeEvent changeEvent = new GemfireChangeEvent("Create", changeDate, keyObject, valueObject);
			sink.input().send(MessageBuilder.withPayload(changeEvent).build());
			assertEquals(1, customerRegion.keySet().size());
		}

		/*
		 * @Test public void loggerTest() { assertNotNull(this.sink.input());
		 * GenericMessage<String> message = new
		 * GenericMessage<String>("this is a test message");
		 * sink.input().send(message);
		 * 
		 * assertNotNull(sink.input()); }
		 */
	}
}
