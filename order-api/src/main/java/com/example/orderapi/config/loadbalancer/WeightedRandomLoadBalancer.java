package com.example.orderapi.config.loadbalancer;

import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.NoopServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.SelectedInstanceCallback;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;

public class WeightedRandomLoadBalancer implements ReactorServiceInstanceLoadBalancer {
	private static final Log log = LogFactory.getLog(WeightedRandomLoadBalancer.class);

	private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;

	private final String serviceId;

	private final WeightedStrategy weightedStrategy;

	public WeightedRandomLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider, String serviceId, WeightedStrategy weightedStrategy) {
		this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
		this.serviceId = serviceId;
		this.weightedStrategy = weightedStrategy;
	}

	@Override
	public Mono<Response<ServiceInstance>> choose(Request request) {
		final ServiceInstanceListSupplier supplier = this.serviceInstanceListSupplierProvider
				.getIfAvailable(NoopServiceInstanceListSupplier::new);
		return supplier.get(request).next().map(serviceInstances -> processInstanceResponse(supplier, serviceInstances));
	}

	Tuple2<NavigableMap<Integer, ServiceInstance>, Integer> buildNavigableMap(List<ServiceInstance> instances) {
		int totalWeight = 0;
		final NavigableMap<Integer, ServiceInstance> navigableMap = new TreeMap<>();
		for (ServiceInstance instance : instances) {
			int weight = this.weightedStrategy.weight(instance);
			totalWeight += weight;
			navigableMap.put(totalWeight, instance);
		}
		return Tuples.of(navigableMap, totalWeight);
	}

	private Response<ServiceInstance> processInstanceResponse(ServiceInstanceListSupplier supplier,
			List<ServiceInstance> serviceInstances) {
		final Response<ServiceInstance> serviceInstanceResponse = getInstanceResponse(serviceInstances);
		if (supplier instanceof SelectedInstanceCallback && serviceInstanceResponse.hasServer()) {
			((SelectedInstanceCallback) supplier).selectedServiceInstance(serviceInstanceResponse.getServer());
		}
		return serviceInstanceResponse;
	}

	private Response<ServiceInstance> getInstanceResponse(List<ServiceInstance> instances) {
		if (instances.isEmpty()) {
			if (log.isWarnEnabled()) {
				log.warn("No servers available for service: " + serviceId);
			}
			return new EmptyResponse();
		}
		final Tuple2<NavigableMap<Integer, ServiceInstance>, Integer> navigableMapAndWeight = buildNavigableMap(instances);
		final NavigableMap<Integer, ServiceInstance> navigableMap = navigableMapAndWeight.getT1();
		final Integer totalWeight = navigableMapAndWeight.getT2();
		final int index = ThreadLocalRandom.current().nextInt(totalWeight);
		final ServiceInstance instance = navigableMap.ceilingEntry(index).getValue();
		return new DefaultResponse(instance);
	}
}
