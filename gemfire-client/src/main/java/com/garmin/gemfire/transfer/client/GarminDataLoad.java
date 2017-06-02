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
		logger.info("Starting - loading of OrderDetail to gemfire ");
		ConfigurableApplicationContext ctx =SpringApplication.run(GarminDataLoad.class, args);
		GarminDataLoad thisObj= ctx.getBean(GarminDataLoad.class);
	//	thisObj.putTest();     
		thisObj.putAllTest();
	//	thisObj.putTestWithSource();
		logger.info("Completed - loading of OrderDetail to gemfire");
	 }
	
	
	public void putAllTest(){
		
		Map<Integer,Customer> orderDetails=new HashMap<Integer,Customer>();
		
		for (int i=1;i<=10;i++) {
			Customer orderDetail=new Customer();
			Integer key =nextInteger();
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

	public void putTest(){
		List<Customer> custList=new ArrayList<Customer>();
		for (int i=1;i<10;i++) {
			Customer orderDetail=new Customer();
			orderDetail.setCustomerNumber(nextInteger());
			orderDetail.setOrderNumber(nextInteger());
			orderDetail.setOrderDate(new Date());
			orderDetail.setCustomerNumber(nextInteger());
			orderDetail.setShipDate(new Date());
			orderDetail.setShippingCost(randomFloat());
			geodeService.putOrder(orderDetail.getCustomerNumber(),orderDetail);
			custList.add(orderDetail);
		}
		
		for(Customer cust:custList){
			geodeService.destroyOrder(cust.getCustomerNumber(),TransferConstants.UPDATE_SOURCE);
		}
		
		for(Customer cust:custList){
			geodeService.removeOrder(cust.getCustomerNumber());
		}
		
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
			geodeService.putOrder(orderDetail.getCustomerNumber(),orderDetail,TransferConstants.UPDATE_SOURCE);
			custList.add(orderDetail);
		}
		
		for(Customer cust:custList){
			geodeService.destroyOrder(cust.getCustomerNumber(),TransferConstants.UPDATE_SOURCE);
		}
	}
	
	public void removeTest(){
		geodeService.removeOrder(new Integer("47212243"));
	}
	
	public void destroyTest(Integer key){
		geodeService.destroyOrder(new Integer("47212243"));
	}
	
	public void destroyTestWithSource(Integer key){
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

 