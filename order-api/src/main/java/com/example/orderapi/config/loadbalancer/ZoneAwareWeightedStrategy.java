package com.example.orderapi.config.loadbalancer;

import java.time.Duration;
import java.util.Objects;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.CompletionContext;
import org.springframework.cloud.client.loadbalancer.CompletionContext.Status;
import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycle;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.client.loadbalancer.ResponseData;

public class ZoneAwareWeightedStrategy implements WeightedStrategy, LoadBalancerLifecycle<RequestData, ResponseData, ServiceInstance> {
	private final Logger log = LoggerFactory.getLogger(ZoneAwareWeightedStrategy.class);

	private final String zone;

	private final Cache<String, Boolean> failed = Caffeine.newBuilder()
			.expireAfterWrite(Duration.ofSeconds(30))
			.build();

	public ZoneAwareWeightedStrategy(String zone) {
		this.zone = zone;
	}

	@Override
	public int weight(ServiceInstance instance) {
		if (this.failed.getIfPresent(instance.getInstanceId()) != null) {
			return 1;
		}
		if (Objects.equals(this.zone, instance.getMetadata().get("zone"))) {
			return WeightedStrategy.DEFAULT_WEIGHT * 100;
		}
		else {
			return WeightedStrategy.DEFAULT_WEIGHT;
		}
	}

	@Override
	public void onComplete(CompletionContext<ResponseData, ServiceInstance, RequestData> completionContext) {
		final Response<ServiceInstance> response = completionContext.getLoadBalancerResponse();
		final ResponseData responseData = completionContext.getClientResponse();
		if (completionContext.status() == Status.FAILED || (responseData != null && responseData.getHttpStatus().is5xxServerError())) {
			log.warn("Mark {} as failed.", response.getServer());
			this.failed.put(response.getServer().getInstanceId(), true);
		}
	}

	@Override
	public void onStart(Request<RequestData> request) {
		// NO-OP
	}

	@Override
	public void onStartRequest(Request<RequestData> request, Response<ServiceInstance> lbResponse) {
		// NO-OP
	}
}
