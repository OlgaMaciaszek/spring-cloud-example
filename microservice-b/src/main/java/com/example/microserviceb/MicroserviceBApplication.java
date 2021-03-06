package com.example.microserviceb;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableEurekaClient
@EnableRetry
public class MicroserviceBApplication {

	@Bean("loadBalancedRestTemplate")
	@LoadBalanced
	public RestTemplate loadBalancedRestTemplate(RestTemplateBuilder restTemplateBuilder) {
		return restTemplateBuilder.build();
	}

	@RestController
	class SampleController {

		private final RestTemplate loadBalancedRestTemplate;
		private final String instanceId;

		SampleController(@LoadBalanced RestTemplate loadBalancedRestTemplate, @Value("${spring.application.instance-id}") String instanceId) {
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
