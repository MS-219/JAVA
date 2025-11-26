package com.yinlian.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebConfig.class);

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射 /device/faces/** 到本地 data/faces 目录
        Path faceDir = Paths.get(System.getProperty("user.dir"), "data", "faces");
        String facePath = "file:" + faceDir.toAbsolutePath().toString() + "/";
        
        logger.info("Mapping /device/faces/** to {}", facePath);
        
        registry.addResourceHandler("/device/faces/**")
                .addResourceLocations(facePath);
    }
}
