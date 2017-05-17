package com.garmin.gemfire;
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.garmin.gemfire.transfer.model.Customer;
import com.garmin.server.gemfire.util.JSONTypedFormatter;
import com.gemstone.gemfire.cache.Declarable;
import com.gemstone.gemfire.cache.EntryEvent;
import com.gemstone.gemfire.cache.util.CacheListenerAdapter;
import com.gemstone.gemfire.pdx.JSONFormatter;
import com.gemstone.gemfire.pdx.PdxInstance;

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
		
		String region= event.getRegion().getName();
		String operation=event.getOperation().toString();
		String jsonTransport = JSONTypedFormatter.toJsonTransport(event.getKey().toString(), event.getNewValue(), event.getOperation().toString(), event.getRegion().getName()); 
		Object newObj=event.getNewValue();
		sendToKafka(jsonTransport,"gemtesttopic");		
			
		/* JSONFormatter.toJSON(pdxInstance) doesn't work either
		byte[] byteArray=JSONFormatter.toJSONByteArray(pdxInstance);
		LOGGER.info("Converted the PDX to JSON byte array :");
		sendToKafka(byteArray,"gemtesttopic");		
		*/
		
	}
	
	private void sendToKafka(byte[] byteArray, String topicName) {
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
	
	private void sendToKafka(String message, String topicName) {
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
