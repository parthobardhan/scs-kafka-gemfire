package com.garmin.gemfire.transfer.client.service;

import java.util.List;
import java.util.Map;

import com.garmin.gemfire.transfer.model.Customer;



public interface IGeodeService {

	public Customer putOrder(String key,Customer orderDetail);
	

	public Customer putOrder(String key,Customer orderDetail, String source);
	

	public Customer removeOrder(String key);
	
	
	public Customer destroyOrder(String key);
	
	public Customer destroyOrder(String key, String source);

	public void putOrderAll(Map<String,Customer> orderDetails);
	
	public void removeOrders(List<String> orderKeys);
	
}
