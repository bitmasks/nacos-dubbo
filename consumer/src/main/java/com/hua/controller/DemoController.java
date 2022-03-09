package com.hua.controller;

import com.hua.service.ApiService;

import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

    @DubboReference
    private ApiService apiService;

    @GetMapping
    public String getInfo() {
        return "consumer调用成功 -> "+apiService.invoke();
    }

}
