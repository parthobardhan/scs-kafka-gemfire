package com.garmin.gemfire.transfer.model;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Date;

import com.gemstone.gemfire.DataSerializable;
import com.gemstone.gemfire.DataSerializer;

public class GemfireChangeEvent implements DataSerializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -5395498707238543187L;
	private String operation;
	private Date lastUpdated;
	private Object eventKey;
	private Object eventObject;
	
	public GemfireChangeEvent(String operation, Date lastUpdated, Object eventObject, Object eventKey) {
		super();
		this.operation = operation;
		this.lastUpdated = lastUpdated;
		this.eventObject = eventObject;
		this.eventKey = eventKey;
	}

	public GemfireChangeEvent(String operation, Date lastUpdated, Object eventKey) {
		super();
		this.operation = operation;
		this.lastUpdated = lastUpdated;
		this.eventKey = eventKey;
	}

	@Override
	public void fromData(DataInput input) throws IOException, ClassNotFoundException {
		operation = DataSerializer.readString(input);
		lastUpdated = DataSerializer.readDate(input);
		eventKey = DataSerializer.readObject(input);
		if (!operation.equals("Delete")){
			eventObject = DataSerializer.readObject(input);
		}
	}

	@Override
	public void toData(DataOutput output) throws IOException {
		DataSerializer.writeString(operation, output);
		DataSerializer.writeDate(lastUpdated, output);
		DataSerializer.writeObject(eventKey, output);
		if (!operation.equals("Delete")) {
			DataSerializer.writeObject(eventObject, output);			
		}
	}
	
	public String getOperation() {
		return operation;
	}

	public Date getLastUpdated() {
		return lastUpdated;
	}

	public Object getEventKey() {
		return eventKey;
	}

	public Object getEventObject() {
		return eventObject;
	}
}
