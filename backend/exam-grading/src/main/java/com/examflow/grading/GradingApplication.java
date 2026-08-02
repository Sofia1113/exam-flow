package com.examflow.grading;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 阅卷成绩服务入口。
 */
@SpringBootApplication
@MapperScan("com.examflow.grading.mapper")
@EnableFeignClients
public class GradingApplication {

    public static void main(String[] args) {
        SpringApplication.run(GradingApplication.class, args);
    }
}
