package com.garmin.gemfire.transfer.subscriber;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.garmin.gemfire.transfer.model.GemfireChangeEvent;
import com.gemstone.gemfire.cache.Region;

//@RunWith(SpringJUnit4ClassRunner.class)
//@SpringBootTest(classes = { KafkaSubscriberApplication.class }, value = { "gemfire.regionName=customer" })
@DirtiesContext
public class KafkaSubscriberApplicationTests {

	@Autowired
	protected Sink sink;

	@Autowired
	protected MessageCollector messageCollector;

	@Autowired
	@Qualifier("customer")
	protected Region customerRegion;

//	@Test
	public void testAddCustomer1() throws InterruptedException {
		customerRegion.clear();
		Object keyObject = new String("Key");
		Object valueObject = new String("Value");
		Date changeDate = new Date();
		GemfireChangeEvent changeEvent = new GemfireChangeEvent("Create", changeDate, keyObject, valueObject);
		sink.input().send(MessageBuilder.withPayload(changeEvent).build());
		assertThat(customerRegion.get("Key"),is("Value"));
		assertEquals(1, customerRegion.keySet().size());
	}
}
