package com.garmin.gemfire.transfer.subscriber;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.garmin.gemfire.transfer.util.JSONTypedFormatter;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.client.ClientCache;
import com.gemstone.gemfire.cache.client.ClientCacheFactory;
import com.gemstone.gemfire.pdx.PdxInstance;

//@RunWith(SpringJUnit4ClassRunner.class)
//@SpringBootTest(classes = { KafkaSubscriberApplication.class })
@DirtiesContext
public class KafkaSubscriberIntegrationTest {

	@Autowired
	protected Sink sink;

	@Autowired
	protected MessageCollector messageCollector;

//	@Test
	public void testAddCustomer1() throws InterruptedException, IOException {
		ClientCacheFactory ccf = new ClientCacheFactory();
		ClientCache cc = ccf.create();
		PdxInstance pi = cc.createPdxInstanceFactory("com.company.DomainObject").writeInt("id", 37)
				.markIdentityField("id").writeString("name", "Mike Smith").create();
		String json = JSONTypedFormatter.toJsonTransport("key", "java.lang.String", pi, pi.getClass().getName(), "Update", "latestTimestamp", new Date().getTime(),0l);
		sink.input().send(MessageBuilder.withPayload(json).build());
	}
}
