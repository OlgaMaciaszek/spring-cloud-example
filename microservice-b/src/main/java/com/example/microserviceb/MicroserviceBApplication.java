package com.example.microserviceb;

import java.time.LocalDateTime;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerProperties;
import org.springframework.cloud.loadbalancer.cache.LoadBalancerCacheManager;
import org.springframework.cloud.loadbalancer.core.CachingServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.DiscoveryClientServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.HealthCheckServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
@EnableEurekaClient
@EnableRetry
public class MicroserviceBApplication {

	@LoadBalanced
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	public ServiceInstanceListSupplier discoveryClientServiceInstanceListSupplier(
			DiscoveryClient discoveryClient, Environment environment,
			LoadBalancerProperties loadBalancerProperties,
			ApplicationContext context,
			WebClient.Builder webClientBuilder) {
		DiscoveryClientServiceInstanceListSupplier firstDelegate = new DiscoveryClientServiceInstanceListSupplier(
				discoveryClient, environment);
		HealthCheckServiceInstanceListSupplier delegate = new HealthCheckServiceInstanceListSupplier(firstDelegate,
				loadBalancerProperties.getHealthCheck(), webClientBuilder.build());
		ObjectProvider<LoadBalancerCacheManager> cacheManagerProvider = context
				.getBeanProvider(LoadBalancerCacheManager.class);
		if (cacheManagerProvider.getIfAvailable() != null) {
			return new CachingServiceInstanceListSupplier(delegate,
					cacheManagerProvider.getIfAvailable());
		}
		return delegate;
	}


	@RestController
	class SampleController {

		private final RestTemplate loadBalancedRestTemplate;

		private final String instanceId;

		SampleController(RestTemplate loadBalancedRestTemplate, @Value("${spring.application.instance-id}") String instanceId) {
			this.loadBalancedRestTemplate = loadBalancedRestTemplate;
			this.instanceId = instanceId;
		}

		@GetMapping("/hello")
		public String hello() {
			return "Hello from " + instanceId + "@" + LocalDateTime.now();
		}

		@GetMapping("/{microservice}")
		public String microservice(@PathVariable("microservice") String microservice) {
			return loadBalancedRestTemplate.getForObject("http://" + microservice + "/hello", String.class);
		}

	}

	public static void main(String[] args) {
		SpringApplication.run(MicroserviceBApplication.class, args);
	}

}
