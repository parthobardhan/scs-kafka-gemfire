package com.garmin.gemfire.transfer.client.service;

import java.util.List;
import java.util.Map;

import com.garmin.gemfire.transfer.model.Customer;



public interface IGeodeService {

	public Customer putOrder(Integer key,Customer orderDetail);
	

	public Customer putOrder(Integer key,Customer orderDetail, String source);
	

	public Customer removeOrder(Integer key);
	
	
	public Customer destroyOrder(Integer key);
	
	public Customer destroyOrder(Integer key, String source);

	public void putOrderAll(Map<Integer,Customer> orderDetails);
	
	public void removeOrders(List<Integer> orderKeys);
	
}
