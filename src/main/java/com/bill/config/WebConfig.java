package com.bill.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.resource.ResourceUrlEncodingFilter;

@Configuration
public class WebConfig {

    @Bean
    public ResourceUrlEncodingFilter resourceUrlEncodingFilter() {
        return new ResourceUrlEncodingFilter();
    }
}
