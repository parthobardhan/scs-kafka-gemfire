package com.garmin.gemfire.transfer.client.service;



import java.util.Map;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.garmin.gemfire.transfer.model.Customer;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.client.ClientCache;
import com.gemstone.gemfire.cache.client.ClientCacheFactory;
import com.gemstone.gemfire.pdx.ReflectionBasedAutoSerializer;


@Service
public class GeodeServiceImpl implements IGeodeService {
	
	private static final Logger logger = LoggerFactory.getLogger(GeodeServiceImpl.class);
	
	public static ClientCache cache=null;
	
	private static Region<Integer, Customer> region=null;
	
	public GeodeServiceImpl(){
		// Create a client cache
		logger.info("Loading the geode client cache file");
	    cache = new ClientCacheFactory()
	      .set("cache-xml-file", "client/geode-client-cache.xml")
	      .setPdxReadSerialized(false)
	      .setPdxSerializer(new ReflectionBasedAutoSerializer("com.garmin.gemfire.transfer.model.*"))
	      .create();
	    region=cache.getRegion("customer");	
	}



	public Customer putOrder(Integer key, Customer orderDetail) {
		logger.debug("Put :"+key+" into region :orderDetail");
		return region.put(key, orderDetail);
	}

	public void putOrderAll(Map<Integer,Customer> orderDetails) {
		region.putAll(orderDetails);
	}

	
}
