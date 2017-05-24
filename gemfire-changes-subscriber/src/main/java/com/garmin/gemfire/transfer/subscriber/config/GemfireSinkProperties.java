package com.garmin.gemfire.transfer.subscriber.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("gemfire")
public class GemfireSinkProperties {

	/**
	 * The region name.
	 */
	private String regionName;

	public String getRegionName() {
		return regionName;
	}

	public void setRegionName(String regionName) {
		this.regionName = regionName;
	}
}
