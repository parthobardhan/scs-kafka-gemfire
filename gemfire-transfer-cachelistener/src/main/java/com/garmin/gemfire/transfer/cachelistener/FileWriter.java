package com.garmin.gemfire.transfer.cachelistener;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.garmin.gemfire.transfer.util.JSONTypedFormatter;
import com.gemstone.gemfire.cache.Declarable;
import com.gemstone.gemfire.cache.EntryEvent;
import com.gemstone.gemfire.cache.util.CacheListenerAdapter;
import com.gemstone.gemfire.internal.cache.EntryEventImpl;



public class FileWriter extends CacheListenerAdapter implements Declarable {

	private static final Logger LOGGER = LoggerFactory.getLogger(FileWriter.class);

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
		
//		if (event.isCallbackArgumentAvailable()) {
//			if (event.getCallbackArgument().toString().equals("SOURCE: KAFKA")) return;
//		}
		
		if (event.getKey().toString().startsWith("MONITORING-")) return;
		// auto.create.topics.enable to create topics 
		
		String region= event.getRegion().getName();
		Long now = System.currentTimeMillis();
		EntryEventImpl eei = (EntryEventImpl)event;
		now = eei.getVersionTag().getVersionTimeStamp();
		Long regionVersion = eei.getVersionTag().getRegionVersion();
		String jsonTransport;
		try {
			jsonTransport = JSONTypedFormatter.toJsonTransport(event.getKey(), event.getKey().getClass().getName(), event.getNewValue(), event.getNewValue().getClass().getName(), event.getOperation().toString(), event.getRegion().getName(),now, regionVersion);			
			sendToFile(jsonTransport);		
		} catch (JsonProcessingException e) {
			LOGGER.error("Error while parsing JSON object :"+event.getKey().toString()+", for a region :"+region);
			e.printStackTrace();
		} 
		
	}
	
	File capture = new File("/srv/gemfire/capture.log");
	private void sendToFile(String msg) {
		try {
			msg += "\n";
			FileChannel channel = FileChannel.open(capture.toPath(), StandardOpenOption.WRITE, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
			FileLock lock = channel.lock();
			ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
			channel.write(buffer);
			lock.release();			
			channel.close();
		} catch (Exception ex) {
			LOGGER.error("Unexpected error",ex);
		} 
	}
	
	
	public void init(Properties arg0) {
	}

}
