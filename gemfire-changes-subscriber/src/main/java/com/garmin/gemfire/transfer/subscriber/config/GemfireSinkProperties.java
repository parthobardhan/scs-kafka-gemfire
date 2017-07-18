package com.garmin.gemfire.transfer.subscriber.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public class GemfireSinkProperties {

	private String clientCacheXmlFile;

	public String getClientCacheXmlFile() {
		return clientCacheXmlFile;
	}

	public void setClientCacheXmlFile(String clientCacheXmlFile) {
		this.clientCacheXmlFile = clientCacheXmlFile;
	}
}
