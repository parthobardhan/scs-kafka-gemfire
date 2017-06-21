package com.garmin.gemfire.dataremover;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@EnableAutoConfiguration
@SpringBootApplication
public class GemfireDataRemover {
	private static final Logger logger = LoggerFactory.getLogger(GemfireDataRemover.class);
	private static final Random random = new Random();

	@Autowired
	private GemfireRemoveOperations gemfireRemoveOperations;

	public static void main(String[] args) {
		ConfigurableApplicationContext ctx = SpringApplication.run(GemfireDataRemover.class, args);
		GemfireDataRemover thisGemfireDataRemover = ctx.getBean(GemfireDataRemover.class);
		thisGemfireDataRemover.removeExistingKeyToTestREMOVEOperation("1");
		thisGemfireDataRemover.destroyExistingKeyToTestDESTROYOperation("2");
		List<String> keys = new ArrayList<String>();
		keys.add("3");
		keys.add("4");
		thisGemfireDataRemover.removeMultipleKeysToTestREMOVEALLOperation(keys);
	}

	private void removeExistingKeyToTestREMOVEOperation(String key){
		logger.info("Removing an object to gemfire for REMOVE operation with key " + key);
		gemfireRemoveOperations.removeCustomer(key);
	}
	
	private void destroyExistingKeyToTestDESTROYOperation(String key){
		logger.info("Removing an object to gemfire for DESTROY operation with key " + key);
		gemfireRemoveOperations.destroyCustomer(key);
	}

	private void removeMultipleKeysToTestREMOVEALLOperation(List<String> keys){
		logger.info("Removing an object to gemfire for REMOVEALL operation with keys " + keys);
		gemfireRemoveOperations.removeCustomers(keys);
	}
}
