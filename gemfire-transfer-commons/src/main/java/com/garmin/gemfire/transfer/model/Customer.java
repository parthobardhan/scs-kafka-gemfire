package com.garmin.gemfire.transfer.model;

import java.util.Date;
public class Customer {

	private Integer orderNumber;
	private Date orderDate;
	private float shippingCost;
	private Date shipDate;
	private Integer customerNumber;
	private float totalPrice;
	
	
	public Integer getOrderNumber() {
		return orderNumber;
	}
	public void setOrderNumber(Integer orderNumber) {
		this.orderNumber = orderNumber;
	}
	public Date getOrderDate() {
		return orderDate;
	}
	public void setOrderDate(Date orderDate) {
		this.orderDate = orderDate;
	}
	public float getShippingCost() {
		return shippingCost;
	}
	public void setShippingCost(float shippingCost) {
		this.shippingCost = shippingCost;
	}
	public Date getShipDate() {
		return shipDate;
	}
	public void setShipDate(Date shipDate) {
		this.shipDate = shipDate;
	}
	public Integer getCustomerNumber() {
		return customerNumber;
	}
	public void setCustomerNumber(Integer customerNumber) {
		this.customerNumber = customerNumber;
	}
	public float getTotalPrice() {
		return totalPrice;
	}
	public void setTotalPrice(float totalPrice) {
		this.totalPrice = totalPrice;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((customerNumber == null) ? 0 : customerNumber.hashCode());
		result = prime * result + ((orderDate == null) ? 0 : orderDate.hashCode());
		result = prime * result + ((orderNumber == null) ? 0 : orderNumber.hashCode());
		result = prime * result + ((shipDate == null) ? 0 : shipDate.hashCode());
		result = prime * result + Float.floatToIntBits(shippingCost);
		result = prime * result + Float.floatToIntBits(totalPrice);
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
		Customer other = (Customer) obj;
		if (customerNumber == null) {
			if (other.customerNumber != null)
				return false;
		} else if (!customerNumber.equals(other.customerNumber))
			return false;
		if (orderDate == null) {
			if (other.orderDate != null)
				return false;
		} else if (!orderDate.equals(other.orderDate))
			return false;
		if (orderNumber == null) {
			if (other.orderNumber != null)
				return false;
		} else if (!orderNumber.equals(other.orderNumber))
			return false;
		if (shipDate == null) {
			if (other.shipDate != null)
				return false;
		} else if (!shipDate.equals(other.shipDate))
			return false;
		if (Float.floatToIntBits(shippingCost) != Float.floatToIntBits(other.shippingCost))
			return false;
		if (Float.floatToIntBits(totalPrice) != Float.floatToIntBits(other.totalPrice))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "Customer [orderNumber=" + orderNumber + ", orderDate=" + orderDate + ", shippingCost="
				+ shippingCost + ", shipDate=" + shipDate + ", customerNumber=" + customerNumber + ", totalPrice="
				+ totalPrice + "]";
	}
	
}