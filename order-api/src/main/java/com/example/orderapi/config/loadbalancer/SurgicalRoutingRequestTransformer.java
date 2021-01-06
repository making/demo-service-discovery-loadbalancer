package com.example.orderapi.config.loadbalancer;

import java.util.Map;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequestTransformer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.support.HttpRequestWrapper;

public class SurgicalRoutingRequestTransformer implements LoadBalancerRequestTransformer {
	private static final String SURGICAL_ROUTING_HEADER_NAME = "surgical_routing_header_name";

	private static final String SURGICAL_ROUTING_HEADER_VALUE = "surgical_routing_header_value";

	@Override
	public HttpRequest transformRequest(HttpRequest request, ServiceInstance instance) {
		final Map<String, String> metadata = instance.getMetadata();
		if (metadata.containsKey(SURGICAL_ROUTING_HEADER_NAME) && metadata.containsKey(SURGICAL_ROUTING_HEADER_VALUE)) {
			final String surgicalRoutingHeaderName = metadata.get(SURGICAL_ROUTING_HEADER_NAME);
			final String surgicalRoutingHeaderValue = metadata.get(SURGICAL_ROUTING_HEADER_VALUE);
			return new HttpRequestWrapper(request) {
				@Override
				public HttpHeaders getHeaders() {
					final HttpHeaders headers = new HttpHeaders();
					headers.putAll(super.getHeaders());
					headers.add(surgicalRoutingHeaderName, surgicalRoutingHeaderValue);
					return headers;
				}
			};
		}
		return request;
	}
}
