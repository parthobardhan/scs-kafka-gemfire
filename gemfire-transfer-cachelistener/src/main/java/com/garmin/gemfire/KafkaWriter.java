package com.garmin.gemfire;
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.garmin.gemfire.transfer.util.JSONTypedFormatter;
import com.gemstone.gemfire.cache.Declarable;
import com.gemstone.gemfire.cache.EntryEvent;
import com.gemstone.gemfire.cache.util.CacheListenerAdapter;



public class KafkaWriter extends CacheListenerAdapter implements Declarable {

	private static final Logger LOGGER = LoggerFactory.getLogger(KafkaWriter.class);
	private static final Properties configProperties = new Properties();

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
		// auto.create.topics.enable to create topics 
		
		String region= event.getRegion().getName();
		Long now = System.currentTimeMillis();
		String jsonTransport;
		try {
			jsonTransport = JSONTypedFormatter.toJsonTransport(event.getKey().toString(), event.getNewValue(), event.getOperation().toString(), event.getRegion().getName(),now);
			sendToKafka("gemtesttopic",jsonTransport);		
		} catch (JsonProcessingException e) {
			LOGGER.error("Error while parsing JSON object :"+event.getKey().toString()+", for a region :"+region);
			e.printStackTrace();
		} 
		
	}
	
	private void sendToKafka(String topicName, byte[] byteArray) {
	    // write to kafka topic
		// Externalize the properties with bookstrap, acks, block on buffer, retries etc.
		org.apache.kafka.clients.producer.Producer producer = new KafkaProducer<String, String>(configProperties);
		try{
		    ProducerRecord<String, byte[]> rec = new ProducerRecord<String, byte[]>(topicName, byteArray);
		    producer.send(rec);
	    }catch(Exception e){
	    	LOGGER.error("Error while sending data to kafka :"+e.getMessage());
	    	e.printStackTrace();
	    }finally{
	    	producer.close();
	    }
	}
	
	private void sendToKafka(String topicName, String message) {
	    // write to kafka topic
		// Externalize the properties with bookstrap, acks, block on buffer, retries etc.
		org.apache.kafka.clients.producer.Producer producer = new KafkaProducer<String, String>(configProperties);
		try{
		    ProducerRecord<String, String> rec = new ProducerRecord<String, String>(topicName, message);
		    producer.send(rec);
	    }catch(Exception e){
	    	LOGGER.error("Error while sending data to kafka :"+e.getMessage());
	    	e.printStackTrace();
	    }finally{
	    	producer.close();
	    }
	}
	
	public void init(Properties arg0) {
		//Configure the Producer
	    configProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"olaxda-itwgfkafka00:9092,olaxda-itwgfkafka01:9092");
	    configProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.ByteArraySerializer");
	    configProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringSerializer");
	  //  configProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.ByteArraySerializer");
	}

}
