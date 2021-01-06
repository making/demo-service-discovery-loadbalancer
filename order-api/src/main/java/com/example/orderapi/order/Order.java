package com.example.orderapi.order;

import java.math.BigDecimal;
import java.util.UUID;

public class Order {
	private final UUID id;

	private final BigDecimal price;

	public Order(UUID id, BigDecimal price) {
		this.id = id;
		this.price = price;
	}

	public UUID getId() {
		return id;
	}

	public BigDecimal getPrice() {
		return price;
	}
}
