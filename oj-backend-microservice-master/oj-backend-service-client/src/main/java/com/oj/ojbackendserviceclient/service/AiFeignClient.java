package com.oj.ojbackendserviceclient.service;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "oj-backend-ai-service", path = "/api/ai/inner")
public interface AiFeignClient {
    @GetMapping("/addQuestion")
    String addQuestion();
}
