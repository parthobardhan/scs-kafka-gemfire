package com.garmin.gemfire.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.gemfire.CacheFactoryBean;

import com.gemstone.gemfire.pdx.PdxSerializer;
import com.gemstone.gemfire.pdx.ReflectionBasedAutoSerializer;

@Configuration
@ComponentScan(basePackages = { "com.garmin.gemfire.server" })
public class GemFireServerBootConfig {

	@Bean
	CacheFactoryBean gemfireCache(PdxSerializer pdxSerializer) {
		CacheFactoryBean gemfireCache = new CacheFactoryBean();
		gemfireCache.setPdxSerializer(pdxSerializer);
  		return gemfireCache; 
	}

	@Bean
	PdxSerializer pdxSerializer() {
		PdxSerializer pdxSerializer = new ReflectionBasedAutoSerializer(".*");
		return pdxSerializer;
	}
}