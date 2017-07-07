package com.garmin.gemfire.transfer.client;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.garmin.gemfire.transfer.client.service.IGeodeService;
import com.garmin.gemfire.transfer.common.TransferConstants;
import com.garmin.gemfire.transfer.model.Customer;




@EnableAutoConfiguration
@SpringBootApplication
public class GarminDataLoad {

	private static final Logger logger = LoggerFactory.getLogger(GarminDataLoad.class);
	private static long minimum=1;
	private static long maximum=100000001;
	private static final Random random = new Random();
	  
	@Autowired
	private IGeodeService geodeService;
	
	public static void main(String[] args) throws Exception {
		logger.info("Starting - loading of Customer to gemfire ");
		ConfigurableApplicationContext ctx =SpringApplication.run(GarminDataLoad.class, args);
		GarminDataLoad thisObj= ctx.getBean(GarminDataLoad.class);
	//	thisObj.putDestroyRemoveTest();     
		thisObj.putAllTest();
	//	thisObj.putTestWithSource();
		logger.info("Completed - loading of Customer to gemfire");
	 }
	
	
	public void putAllTest(){
		
		Map<String,Customer> orderDetails=new HashMap<String,Customer>();
		
		for (int i=1;i<=10;i++) {
			Customer orderDetail=new Customer();
			String key = new String(nextInteger().toString());
			//orderDetail.setTransId(nextInteger());
			orderDetail.setOrderNumber(nextInteger());
			orderDetail.setOrderDate(new Date());
			orderDetail.setCustomerNumber(nextInteger());
			orderDetail.setShipDate(new Date());
			orderDetail.setShippingCost(randomFloat());
			orderDetails.put(key,orderDetail);
			logger.info("Loading an object to gemfire :"+key);
		}
		geodeService.putOrderAll(orderDetails);
	}
	
	

	public void putDestroyRemoveTest(){
		List<Customer> custList=new ArrayList<Customer>();
		for (int i=1;i<10;i++) {
			Customer orderDetail=new Customer();
			orderDetail.setCustomerNumber(nextInteger());
			orderDetail.setOrderNumber(nextInteger());
			orderDetail.setOrderDate(new Date());
			orderDetail.setCustomerNumber(nextInteger());
			orderDetail.setShipDate(new Date());
			orderDetail.setShippingCost(randomFloat());
			geodeService.putOrder(orderDetail.getCustomerNumber().toString(),orderDetail);
			custList.add(orderDetail);
		}
		
		/*
		// Destroy first 3
		int i=0;
		for(Customer cust:custList){
			if (i++<3) 
				geodeService.destroyOrder(cust.getCustomerNumber(),TransferConstants.UPDATE_SOURCE);
		}
		// remove next 3
		i=0;
		for(Customer cust:custList){
			if (i++ >= 3 && i < 6)
			geodeService.removeOrder(cust.getCustomerNumber());
		}
		*/
		// removeAll
		List<String> orderKeys=new ArrayList<String>();
		for(Customer cust:custList){
			orderKeys.add(cust.getCustomerNumber().toString());
		}
		geodeService.removeOrders(orderKeys);
		
	}
	
	public void putTestWithSource(){
		
		List<Customer> custList=new ArrayList<Customer>();
		for (int i=1;i<10;i++) {
			Customer orderDetail=new Customer();
			orderDetail.setCustomerNumber(nextInteger());
			orderDetail.setOrderNumber(nextInteger());
			orderDetail.setOrderDate(new Date());
			orderDetail.setCustomerNumber(nextInteger());
			orderDetail.setShipDate(new Date());
			orderDetail.setShippingCost(randomFloat());
			geodeService.putOrder(orderDetail.getCustomerNumber().toString(),orderDetail,TransferConstants.UPDATE_SOURCE);
			custList.add(orderDetail);
		}
		
		for(Customer cust:custList){
			geodeService.destroyOrder(cust.getCustomerNumber().toString(),TransferConstants.UPDATE_SOURCE);
		}
	}
	
	public void removeTest(){
		geodeService.removeOrder("47212243");
	}
	
	public void destroyTest(Integer key){
		geodeService.destroyOrder("47212243");
	}
	
	public void destroyTestWithSource(String key){
		geodeService.destroyOrder(key,TransferConstants.UPDATE_SOURCE);
	}
	
	
	  public Integer nextInteger() {
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

 