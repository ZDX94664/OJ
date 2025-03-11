package com.oj.ojbackendaiservice.controller;


import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import io.github.pigmesh.ai.deepseek.core.DeepSeekClient;
import io.github.pigmesh.ai.deepseek.core.chat.ChatCompletionResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequestMapping("/")
public class AiController {

    @Value("${deepseek.api.key}")
    private String apiKey;
    @Value("${deepseek.api.url}")
    private String apiUrl;
    @Resource
    private DeepSeekClient deepSeekClient;

//    @PostMapping("/chat")
//    public String chat(@RequestBody String message) {
//
//        String json = JSONUtil.toJsonStr(message);
//        String responseStr = HttpUtil.createPost(apiUrl)
//                .header("Content-Type", "application/json")
//                .header("Accept", "application/json")
//                .header("Authorization", "Bearer " + apiKey)
//                .body(json)
//                .execute()
//                .body();
//        return responseStr;
//    }
    // sse 流式返回
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatCompletionResponse> test(String prompt) {
        return deepSeekClient.chatFluxCompletion(prompt);
    }

}

