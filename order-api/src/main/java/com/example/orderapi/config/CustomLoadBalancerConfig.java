package com.example.orderapi.config;

import com.example.orderapi.config.loadbalancer.WeightedRandomLoadBalancer;
import com.example.orderapi.config.loadbalancer.WeightedStrategy;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

public class CustomLoadBalancerConfig {
	@Bean
	public ReactorLoadBalancer<ServiceInstance> weightedRandomLoadBalancer(Environment environment,
			LoadBalancerClientFactory loadBalancerClientFactory, ObjectProvider<WeightedStrategy> weightedStrategyObjectProvider) {
		final String serviceId = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
		final WeightedStrategy weightedStrategy = weightedStrategyObjectProvider.getIfAvailable(() -> WeightedStrategy.DEFAULT);
		return new WeightedRandomLoadBalancer(loadBalancerClientFactory.getLazyProvider(serviceId, ServiceInstanceListSupplier.class), serviceId, weightedStrategy);
	}
}
