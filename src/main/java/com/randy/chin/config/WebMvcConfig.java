package com.randy.chin.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Bean
    public FilterRegistrationBean filterRegistration() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(new SimpleCORSFilter());
        List<String> urlList = new ArrayList<String>();
        urlList.add("/*");
        registration.setUrlPatterns(urlList);
        registration.setName("UrlFilter");
        registration.setOrder(1);
        return registration;
    }
}
