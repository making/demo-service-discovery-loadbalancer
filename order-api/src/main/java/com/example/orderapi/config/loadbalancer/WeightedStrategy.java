package com.example.orderapi.config.loadbalancer;

import org.springframework.cloud.client.ServiceInstance;

public interface WeightedStrategy {
	int DEFAULT_WEIGHT = 100;

	int weight(ServiceInstance instance);

	WeightedStrategy DEFAULT = instance -> DEFAULT_WEIGHT;
}
