package com.oj.ojbackendaiservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
public class OjBackendAiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OjBackendAiServiceApplication.class, args);
    }

}
