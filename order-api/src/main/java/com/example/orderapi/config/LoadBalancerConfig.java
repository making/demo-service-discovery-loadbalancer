package com.example.orderapi.config;

import java.util.Objects;

import com.example.orderapi.config.loadbalancer.SurgicalRoutingRequestTransformer;
import com.example.orderapi.config.loadbalancer.WeightedStrategy;
import com.example.orderapi.config.loadbalancer.ZoneAwareInternalIpRequestTransformer;
import com.example.orderapi.config.loadbalancer.ZoneAwareWeightedStrategy;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.okhttp3.OkHttpMetricsEventListener;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties.Retry.Backoff;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.blocking.retry.BlockingLoadBalancedRetryFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.web.client.RestTemplate;

@Configuration
public class LoadBalancerConfig {

	@LoadBalanced
	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder, OkHttpClient okHttpClient) {
		return builder
				.requestFactory(() -> new OkHttp3ClientHttpRequestFactory(okHttpClient))
				.build();
	}

	@Bean
	public OkHttpClient okHttpClient(MeterRegistry meterRegistry) {
		return new OkHttpClient.Builder()
				.addNetworkInterceptor(new HttpLoggingInterceptor().setLevel(Level.BODY))
				.eventListener(OkHttpMetricsEventListener
						.builder(meterRegistry, "okhttp.requests")
						.tag((request, response) -> Tag.of("cf-app-instance", Objects.requireNonNullElse(request.header("X-Cf-App-Instance"), "none")))
						.uriMapper(request -> request.url().toString())
						.build())
				.build();
	}

	@Bean
	public SurgicalRoutingRequestTransformer surgicalRoutingRequestTransformer() {
		return new SurgicalRoutingRequestTransformer();
	}

	@Bean
	@Profile("internal-ip")
	public ZoneAwareInternalIpRequestTransformer zoneAwareInternalIpRequestTransformer(@Value("${spring.cloud.loadbalancer.zone}") String zone) {
		return new ZoneAwareInternalIpRequestTransformer(zone);
	}

	@Bean
	public LoadBalancedRetryFactory loadBalancedRetryFactory(LoadBalancerProperties properties) {
		return new BlockingLoadBalancedRetryFactory(properties) {
			@Override
			public BackOffPolicy createBackOffPolicy(String service) {
				final Backoff backoff = properties.getRetry().getBackoff();
				if (!backoff.isEnabled()) {
					return new NoBackOffPolicy();
				}
				final ExponentialRandomBackOffPolicy backOffPolicy = new ExponentialRandomBackOffPolicy();
				backOffPolicy.setMaxInterval(backoff.getMaxBackoff().toMillis());
				backOffPolicy.setInitialInterval(backoff.getMinBackoff().toMillis());
				// spring.cloud.loadbalancer.retry.backoff.jitter is not respected.
				return backOffPolicy;
			}
		};
	}

	@Configuration
	@Profile("weighted-random")
	@LoadBalancerClients(defaultConfiguration = CustomLoadBalancerConfig.class)
	static class CustomConfig {
		@Bean
		public WeightedStrategy weightedStrategy(@Value("${spring.cloud.loadbalancer.zone}") String zone) {
			return new ZoneAwareWeightedStrategy(zone);
		}
	}
}
