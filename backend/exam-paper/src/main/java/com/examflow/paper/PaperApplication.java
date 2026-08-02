package com.examflow.paper;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 组卷服务入口。
 */
@SpringBootApplication
@MapperScan("com.examflow.paper.mapper")
@EnableFeignClients
public class PaperApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaperApplication.class, args);
    }
}
