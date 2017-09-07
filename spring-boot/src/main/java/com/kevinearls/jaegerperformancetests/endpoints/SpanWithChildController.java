package com.kevinearls.jaegerperformancetests.endpoints;

import com.kevinearls.jaegerperformancetests.util.BackendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Date;

@RestController
public class SpanWithChildController {
    @Autowired
    private BackendService backendService;

    @Autowired
    private RestTemplate restTemplate;

    @RequestMapping("/spanWithChild")
    public String spanWithChild() throws InterruptedException {
        backendService.action();
        return "Hello from /spanWithChild " + new Date();
    }
}
