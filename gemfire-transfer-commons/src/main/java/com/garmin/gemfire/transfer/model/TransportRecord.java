package com.garmin.gemfire.transfer.model;

public class TransportRecord {
	
	/**
	 * 
	 */
	//private static final long serialVersionUID = 1L;
	private String operation;
	private String region;
	private Object key;
	private String keyType;
	private Long timestamp;
	private Object object;
	private String objectType;
	
	public static final String FIELD_OPERATION = "operation";
	public static final String FIELD_KEY = "key";
	public static final String FIELD_KEY_TYPE = "key__type";
	public static final String FIELD_REGION = "region";
	public static final String FIELD_TIMESTAMP = "timestamp";
	public static final String FIELD_OBJECT = "object";
	public static final String FIELD_OBJECT_TYPE = "object__type";
	
	
	public TransportRecord() {
		super();
		// TODO Auto-generated constructor stub
	}


	public TransportRecord(Object key, String keyType, Object object, String objectType, String region, String operation, Long timestamp) {
		this.operation = operation;
		this.key = key;
		this.keyType = keyType;
		this.region = region;
		this.timestamp = timestamp;
		this.object = object;		
		this.objectType = objectType;
	}
	
	
	public String getOperation() {return operation;}
	public void setOperation(String operation) {this.operation = operation;}
	
	public String getRegion() {return region;}
	public void setRegion(String region) {this.region = region;}
	
	public Object getKey() {return key;}
	public void setKey(Object key) {this.key = key;}

	public String getKeyType() {return keyType;}
	public void setKeyType(String keyType) {this.keyType = keyType;}
	
	public Long getTimestamp() {return timestamp;}
	public void setTimestamp(Long timestamp) {this.timestamp = timestamp;}
	
	public Object getObject() {return object;}
	public void setObject(Object object) {this.object = object;}
	
	public String getObjectType() { return objectType;}
	public void setObjectType(String objectType) {this.objectType = objectType;}

}
