package com.oj.ojbackendserviceclient.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = {"com.oj.ojbackendserviceclient.service"})
public class FeignConfig {
}
