package com.garmin.gemfire.dataremover;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.garmin.gemfire.transfer.model.Customer;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.client.ClientCache;
import com.gemstone.gemfire.cache.client.ClientCacheFactory;
import com.gemstone.gemfire.pdx.ReflectionBasedAutoSerializer;

@Service
public class GemfireRemoveOperations {

	private static final Logger logger = LoggerFactory.getLogger(GemfireRemoveOperations.class);

	public static ClientCache cache = null;

	private static Region<String, Customer> region = null;

	public GemfireRemoveOperations() {
		// Create a client cache
		logger.info("Loading the geode client cache file");
		cache = new ClientCacheFactory().set("cache-xml-file", "client-cache.xml").setPdxReadSerialized(false)
				.setPdxSerializer(new ReflectionBasedAutoSerializer("com.garmin.gemfire.transfer.model.*")).create();
		region = cache.getRegion("customer");
	}

	public Customer removeCustomer(String key) {
		return region.remove(key);
	}

	public Customer destroyCustomer(String key) {
		return region.destroy(key);
	}

	public Customer destroyCustomer(String key, String source) {
		return region.destroy(key, source);
	}

	public void removeCustomers(List<String> keys) {
		region.removeAll(keys);
	}
}
