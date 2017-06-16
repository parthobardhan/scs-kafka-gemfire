package com.garmin.gemfire.transfer.client.service;

import java.util.List;
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

	public static ClientCache cache = null;

	private static Region<Integer, Customer> region = null;

	public GeodeServiceImpl() {
		// Create a client cache
		logger.info("Loading the geode client cache file");
		cache = new ClientCacheFactory().set("cache-xml-file", "client/geode-client-cache.xml")
				.setPdxReadSerialized(false)
				.setPdxSerializer(new ReflectionBasedAutoSerializer("com.garmin.gemfire.transfer.model.*")).create();
		region = cache.getRegion("customer");
	}

	public Customer putOrder(Integer key, Customer orderDetail) {
		logger.debug("Put :" + key + " into region :orderDetail");
		return region.put(key, orderDetail);
	}

	public Customer putOrder(Integer key, Customer orderDetail, String source) {
		logger.debug("Put :" + key + " into region :orderDetail");
		return region.put(key, orderDetail, source);
	}

	public void putOrderAll(Map<Integer, Customer> orderDetails) {
		region.putAll(orderDetails);
	}

	@Override
	public Customer removeOrder(Integer key) {
		return region.remove(key);
	}

	@Override
	public Customer destroyOrder(Integer key) {
		return region.destroy(key);
	}

	@Override
	public Customer destroyOrder(Integer key, String source) {
		return region.destroy(key, source);
	}

	public void removeOrders(List<Integer> orderKeys) {
		region.removeAll(orderKeys);
	}

}
