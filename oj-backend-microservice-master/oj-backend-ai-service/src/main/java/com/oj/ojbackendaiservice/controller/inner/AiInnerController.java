package com.oj.ojbackendaiservice.controller.inner;

import cn.hutool.http.HttpUtil;
import com.oj.ojbackendserviceclient.service.AiFeignClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inner")
public class AiInnerController implements AiFeignClient {
    @Value("${deepseek.api.key}")
    private String apiKey;
    @Value("${deepseek.api.url}")
    private String apiUrl;

    @GetMapping("/addQuestion")
    @Override
    public String addQuestion() {
        String message ="{\n" +
                "  \"messages\": [\n" +
                "    {\n" +
                "      \"content\": \"你是一个算法题库专家，每次都能帮助用户随机生成一道专业的算法题格式如下tags标签里面记得加上难度比如简单中等困难：{\\n  \\\"title\\\": \\\"\\\",\\n  \\\"content\\\": \\\"\\\",\\n  \\\"tags\\\": [\\\"\\\", \\\"\\\"],\\n  \\\"answer\\\": \\\"\\\",\\n  \\\"judgeCase\\\": [\\n    {\\\"input\\\": \\\"\\\", \\\"output\\\": \\\"\\\"},\\n    {\\\"input\\\": \\\"\\\", \\\"output\\\": \\\"\\\"},\\n    {\\\"input\\\": \\\"\\\", \\\"output\\\": \\\"\\\"}\\n  ],\\n  \\\"judgeConfig\\\": {\\n    \\\"timeLimit\\\": ,\\n    \\\"memoryLimit\\\": \\n    \\\"stackLimit\\\": \\n  }\\n}\",\n" +
                "      \"role\": \"system\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"content\": \"请帮我随机生成一道算法题\",\n" +
                "      \"role\": \"user\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"model\": \"deepseek-chat\",\n" +
                "  \"frequency_penalty\": 0,\n" +
                "  \"max_tokens\": 2048,\n" +
                "  \"presence_penalty\": 0,\n" +
                "  \"response_format\": {\n" +
                "    \"type\": \"text\"\n" +
                "  },\n" +
                "  \"stop\": null,\n" +
                "  \"stream\": false,\n" +
                "  \"stream_options\": null,\n" +
                "  \"temperature\": 1,\n" +
                "  \"top_p\": 1,\n" +
                "  \"tools\": null,\n" +
                "  \"tool_choice\": \"none\",\n" +
                "  \"logprobs\": false,\n" +
                "  \"top_logprobs\": null\n" +
                "}";

//        String json = JSONUtil.toJsonStr(message);
        String responseStr = HttpUtil.createPost(apiUrl)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .body(message)
                .execute()
                .body();
        System.out.println(responseStr);
        return responseStr;
    }


}
