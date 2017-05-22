package com.garmin.gemfire.server.initializer;

import org.springframework.data.gemfire.support.SpringContextBootstrappingInitializer;

import com.garmin.gemfire.server.GemFireServerBootApplication;
import com.gemstone.gemfire.internal.ClassPathLoader;

public class GemFireInitializer extends SpringContextBootstrappingInitializer {

	public GemFireInitializer() {
		super();
		setBeanClassLoader(ClassPathLoader.getLatestAsClassLoader());
		register(GemFireServerBootApplication.class);
	}
}
