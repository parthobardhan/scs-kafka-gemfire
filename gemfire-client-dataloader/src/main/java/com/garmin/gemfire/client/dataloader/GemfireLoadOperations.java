package com.garmin.gemfire.client.dataloader;

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
public class GemfireLoadOperations {

	private static final Logger logger = LoggerFactory.getLogger(GemfireLoadOperations.class);

	public static ClientCache cache = null;

	private static Region<Integer, Customer> region = null;

	public GemfireLoadOperations() {
		// Create a client cache
		logger.info("Loading the geode client cache file");
		cache = new ClientCacheFactory().set("cache-xml-file", "client-cache.xml")
				.setPdxReadSerialized(false)
				.setPdxSerializer(new ReflectionBasedAutoSerializer("com.garmin.gemfire.transfer.model.*")).create();
		region = cache.getRegion("customer");
	}

	public Customer putCustomer(Integer key, Customer customer) {
		logger.debug("Put :" + key + " into region :customer");
		return region.put(key, customer);
	}

	public Customer putCustomer(Integer key, Customer customer, String source) {
		logger.debug("Put :" + key + " into region :customer");
		return region.put(key, customer, source);
	}

	public void putAllCustomers(Map<Integer, Customer> orderDetails) {
		region.putAll(orderDetails);
	}

	public void replace(Integer key, Customer customer) {
		region.replace(key, customer);
	}

	public void putIfAbsent(Integer key, Customer customer) {
		region.putIfAbsent(key, customer);
	}

	// public Customer removeOrder(Integer key) {
	// return region.remove(key);
	// }
	//
	// public Customer destroyOrder(Integer key) {
	// return region.destroy(key);
	// }
	//
	// public Customer destroyOrder(Integer key, String source) {
	// return region.destroy(key, source);
	// }
	//
	// public void removeOrders(List<Integer> orderKeys) {
	// region.removeAll(orderKeys);
	// }
}
