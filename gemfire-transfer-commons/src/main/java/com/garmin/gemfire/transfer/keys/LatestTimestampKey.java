package com.garmin.gemfire.transfer.keys;

import java.io.Serializable;

public class LatestTimestampKey implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -1304251371033594766L;

	private String region;
	private Object key;
	
	public LatestTimestampKey(String region, Object key) {
		super();
		this.region = region;
		this.key = key;
	}

	public String getRegion() {
		return region;
	}

	public Object getKey() {
		return key;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((region == null) ? 0 : region.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LatestTimestampKey other = (LatestTimestampKey) obj;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (region == null) {
			if (other.region != null)
				return false;
		} else if (!region.equals(other.region))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "LatestTimestampKey [region=" + region + ", key=" + key + "]";
	}

}
