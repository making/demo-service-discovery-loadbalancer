package com.example.orderapi.order;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {
	private final RestTemplate restTemplate;

	public OrderService(@LoadBalanced RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	public Order createOrder(Order order) {
		try {
			this.restTemplate.postForEntity("http://payment-api", Map.of("price", order.getPrice()), JsonNode.class);
			return order;
		}
		catch (RestClientResponseException e) {
			throw new ResponseStatusException(e.getRawStatusCode(), e.getStatusText(), e);
		}
	}
}
