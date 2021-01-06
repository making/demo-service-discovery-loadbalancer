package com.example.orderapi.order;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {
	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@PostMapping(path = "/")
	public Order placeOrder(@RequestBody JsonNode jsonNode) {
		final Order order = new Order(UUID.randomUUID(), jsonNode.get("price").decimalValue());
		return this.orderService.createOrder(order);
	}
}
