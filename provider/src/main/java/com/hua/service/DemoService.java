package com.hua.service;

import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Value;

@DubboService(loadbalance = "pollingBalance",weight = 1)
public class DemoService implements ApiService {

    @Value("${server.port}")
    public String port;

    @Override
    public String invoke() {
        return "provider调用成功 -> " + port;
    }
}