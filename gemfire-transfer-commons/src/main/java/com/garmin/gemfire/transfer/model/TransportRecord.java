package com.garmin.gemfire.transfer.model;

public class TransportRecord {
	
	/**
	 * 
	 */
	//private static final long serialVersionUID = 1L;
	private String operation;
	private String region;
	private String key;
	private Long timestamp;
	private Object object;
	
	public static final String FIELD_OPERATION = "operation";
	public static final String FIELD_KEY = "key";
	public static final String FIELD_REGION = "region";
	public static final String FIELD_TIMESTAMP = "timestamp";
	public static final String FIELD_OBJECT = "object";
	
	
	public TransportRecord() {
		super();
		// TODO Auto-generated constructor stub
	}


	public TransportRecord(String operation, String key, String region, Long timestamp, Object object) {
		this.operation = operation;
		this.key = key;
		this.region = region;
		this.timestamp = timestamp;
		this.object = object;		
	}
	
	
	public String getOperation() {return operation;}
	public void setOperation(String operation) {this.operation = operation;}
	
	public String getRegion() {return region;}
	public void setRegion(String region) {this.region = region;}
	
	public String getKey() {return key;}
	public void setKey(String key) {this.key = key;}
	
	public Long getTimestamp() {return timestamp;}
	public void setTimestamp(Long timestamp) {this.timestamp = timestamp;}
	
	public Object getObject() {return object;}
	public void setObject(Object object) {this.object = object;}

}
