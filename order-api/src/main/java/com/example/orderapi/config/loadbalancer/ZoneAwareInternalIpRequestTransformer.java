package com.example.orderapi.config.loadbalancer;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequestTransformer;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.util.UriComponentsBuilder;

public class ZoneAwareInternalIpRequestTransformer implements LoadBalancerRequestTransformer {
	private static final String INTERNAL_IP = "internal_ip";

	private static final String INTERNAL_PORT = "internal_port";

	private static final String INTERNAL_SCHEME = "internal_scheme";

	private final String zone;

	public ZoneAwareInternalIpRequestTransformer(String zone) {
		this.zone = zone;
	}

	@Override
	public HttpRequest transformRequest(HttpRequest request, ServiceInstance instance) {
		final Map<String, String> metadata = instance.getMetadata();
		if (Objects.equals(zone, metadata.get("zone")) && metadata.containsKey(INTERNAL_IP) && metadata.containsKey(INTERNAL_PORT)) {
			final String ip = metadata.get(INTERNAL_IP);
			final String port = metadata.get(INTERNAL_PORT);
			final String scheme = metadata.getOrDefault(INTERNAL_SCHEME, "http");
			try {
				return new HttpRequestWrapper(request) {
					@Override
					public URI getURI() {
						return UriComponentsBuilder.fromUri(super.getURI())
								.scheme(scheme)
								.host(ip)
								.port(Integer.parseInt(port))
								.build().toUri();
					}
				};
			}
			catch (NumberFormatException e) {
				return request;
			}
		}
		return request;
	}
}
