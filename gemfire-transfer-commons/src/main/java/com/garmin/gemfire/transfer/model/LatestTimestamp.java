package com.garmin.gemfire.transfer.model;

public class LatestTimestamp {
	private long latestTimestamp;
	private long regionVersion;

	public LatestTimestamp() {
		super();
	}

	public LatestTimestamp(long latestTimestamp, long regionVersion) {
		super();
		this.latestTimestamp = latestTimestamp;
		this.regionVersion = regionVersion;
	}

	public long getLatestTimestamp() {
		return latestTimestamp;
	}

	public long getRegionVersion() {
		return regionVersion;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (latestTimestamp ^ (latestTimestamp >>> 32));
		result = prime * result + (int) (regionVersion ^ (regionVersion >>> 32));
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
		LatestTimestamp other = (LatestTimestamp) obj;
		if (latestTimestamp != other.latestTimestamp)
			return false;
		if (regionVersion != other.regionVersion)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "LatestTimestamp [latestTimestamp=" + latestTimestamp + ", regionVersion=" + regionVersion + "]";
	}
}
