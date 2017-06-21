package com.garmin.gemfire.client.dataloader;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.garmin.gemfire.transfer.model.Customer;
@EnableAutoConfiguration
@SpringBootApplication
public class GemfireDataLoader {
	private static final Logger logger = LoggerFactory.getLogger(GemfireDataLoader.class);
	private static long minimum = 1;
	private static long maximum = 100000001;
	private static final Random random = new Random();

	
	@Autowired
	private GemfireLoadAndGetOperations gemfireOperations;
	
	public static void main(String[] args) {
		ConfigurableApplicationContext ctx = SpringApplication.run(GemfireDataLoader.class, args);
		GemfireDataLoader thisGemfireDataLoader = ctx.getBean(GemfireDataLoader.class);
		thisGemfireDataLoader.putOneObjectToTestCREATEOperation();
		thisGemfireDataLoader.putOneObjectUsingPutToTestUPDATEOperation();
		thisGemfireDataLoader.putTwoNewObjectsUsingPutAllToTestPUTALL_CREATEOperation();
		thisGemfireDataLoader.updateTwoObjectsUsingPutAllTestPUTALL_UPDATEOperation();
		thisGemfireDataLoader.replaceObjectToTestREPLACEOperation();
		thisGemfireDataLoader.putAbsentObjectToTestPUTIFABSENTOperation();
		thisGemfireDataLoader.putExistingObjectToTestPUTIFABSENTOperation();
		thisGemfireDataLoader.doGets();
	}

	private void putOneObjectToTestCREATEOperation() {
		Customer customer = new Customer();
		String key = new String("1");
		customer.setOrderNumber(nextInteger());
		customer.setOrderDate(new Date());
		customer.setCustomerNumber(nextInteger());
		customer.setShipDate(new Date());
		customer.setShippingCost(randomFloat());
		logger.info("Loading an object to gemfire for CREATE operation with key "+key + "and OrderNumber " + customer.getOrderNumber());
		gemfireOperations.putCustomer(key, customer);
	}

	private void putOneObjectUsingPutToTestUPDATEOperation(){
		Customer customer = new Customer();
		String key = new String("1");
		customer.setOrderNumber(nextInteger());
		customer.setOrderDate(new Date());
		customer.setCustomerNumber(nextInteger());
		customer.setShipDate(new Date());
		customer.setShippingCost(randomFloat());
		logger.info("Loading an object to gemfire for UPDATE operation with key "+key + "and OrderNumber " + customer.getOrderNumber());
		gemfireOperations.putCustomer(key, customer);
	}

	private void putTwoNewObjectsUsingPutAllToTestPUTALL_CREATEOperation() {
		Map<String,Customer> customers=new HashMap<String,Customer>();
		
			Customer customer=new Customer();
			String key = new String("2");
			//orderDetail.setTransId(nextInteger());
			customer.setOrderNumber(nextInteger());
			customer.setOrderDate(new Date());
			customer.setCustomerNumber(nextInteger());
			customer.setShipDate(new Date());
			customer.setShippingCost(randomFloat());
			customers.put(key,customer);
			logger.info("Loading an object to gemfire for PUTALL_CREATE operation with key "+key + "and OrderNumber " + customer.getOrderNumber());

			customer=new Customer();
			key = new String("3");
			//orderDetail.setTransId(nextInteger());
			customer.setOrderNumber(nextInteger());
			customer.setOrderDate(new Date());
			customer.setCustomerNumber(nextInteger());
			customer.setShipDate(new Date());
			customer.setShippingCost(randomFloat());
			customers.put(key,customer);
			logger.info("Loading an object to gemfire for PUTALL_CREATE operation with key "+key + "and OrderNumber " + customer.getOrderNumber());
			gemfireOperations.putAllCustomers(customers);
	}
	
	private void updateTwoObjectsUsingPutAllTestPUTALL_UPDATEOperation(){
		Map<String,Customer> customers=new HashMap<String,Customer>();
		
		Customer customer=new Customer();
		String key = new String("2");
		//orderDetail.setTransId(nextInteger());
		customer.setOrderNumber(nextInteger());
		customer.setOrderDate(new Date());
		customer.setCustomerNumber(nextInteger());
		customer.setShipDate(new Date());
		customer.setShippingCost(randomFloat());
		customers.put(key,customer);
		logger.info("Loading an object to gemfire for PUTALL_UPDATE operation with key " + key + "and OrderNumber " + customer.getOrderNumber());

		customer=new Customer();
		key = new String("3");
		//orderDetail.setTransId(nextInteger());
		customer.setOrderNumber(nextInteger());
		customer.setOrderDate(new Date());
		customer.setCustomerNumber(nextInteger());
		customer.setShipDate(new Date());
		customer.setShippingCost(randomFloat());
		customers.put(key,customer);
		logger.info("Loading an object to gemfire for PUTALL_UPDATE operation with key " + key + "and OrderNumber " + customer.getOrderNumber());

		gemfireOperations.putAllCustomers(customers);
	}
	
	private void replaceObjectToTestREPLACEOperation(){
		Customer customer = new Customer();
		String key = new String("1");
		customer.setOrderNumber(nextInteger());
		customer.setOrderDate(new Date());
		customer.setCustomerNumber(nextInteger());
		customer.setShipDate(new Date());
		customer.setShippingCost(randomFloat());
		logger.info("Loading an object to gemfire for REPLACE operation with key "+key + "and OrderNumber " + customer.getOrderNumber());
		gemfireOperations.replace(key, customer);
	}
	
	private void putAbsentObjectToTestPUTIFABSENTOperation(){
		Customer customer = new Customer();
		String key = new String("4");
		customer.setOrderNumber(nextInteger());
		customer.setOrderDate(new Date());
		customer.setCustomerNumber(nextInteger());
		customer.setShipDate(new Date());
		customer.setShippingCost(randomFloat());
		logger.info("Loading an object to gemfire for PUTIFABSENT operation with key "+ key + "and OrderNumber " + customer.getOrderNumber());
		gemfireOperations.putIfAbsent(key, customer);
	}
	
	private void putExistingObjectToTestPUTIFABSENTOperation(){
		Customer customer = new Customer();
		String key = new String("4");
		customer.setOrderNumber(nextInteger());
		customer.setOrderDate(new Date());
		customer.setCustomerNumber(nextInteger());
		customer.setShipDate(new Date());
		customer.setShippingCost(randomFloat());
		logger.info("Loading an EXISTING object to gemfire to test PUTIFABSENT operation with key "+key + "and OrderNumber " + customer.getOrderNumber());
		gemfireOperations.putIfAbsent(key, customer);
	}

	private void doGets(){
		Customer customer1 = gemfireOperations.getCustomer("4");
		Customer customer2 = gemfireOperations.getCustomer("5");
	}
	
	private Integer nextInteger() {
		int range = (int) (maximum - minimum) + 1;
		int min = (int) minimum;
		int value = random.nextInt(range) + min;
		return new Integer(value);
	}

	public Float randomFloat() {
		int range = (int) (maximum - minimum) + 1;
		int min = (int) minimum;
		float value = random.nextFloat();
		return new Float(value);
	}

	public Date randomDate() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -1 * random.nextInt(365));
		return cal.getTime();
	}

}
