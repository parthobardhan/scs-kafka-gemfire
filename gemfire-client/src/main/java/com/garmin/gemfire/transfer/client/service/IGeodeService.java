package com.garmin.gemfire.transfer.client.service;

import java.util.Map;

import com.garmin.gemfire.transfer.model.Customer;



public interface IGeodeService {

	public Customer putOrder(Integer key,Customer orderDetail);
	
	public void putOrderAll(Map<Integer,Customer> orderDetails);
	
}
