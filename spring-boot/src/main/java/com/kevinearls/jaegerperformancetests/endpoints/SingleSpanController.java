package com.kevinearls.jaegerperformancetests.endpoints;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Date;

@RestController
public class SingleSpanController {
    @Autowired
    private RestTemplate restTemplate;

    @RequestMapping("/singleSpan")
    public String singleSpan() throws InterruptedException {
        return "Hello from /singleSpan at " + new Date();
    }
}
