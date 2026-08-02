package com.examflow.exam;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 考试服务入口(核心服务:交卷/保存链路最高优先级)。
 */
@SpringBootApplication
@MapperScan("com.examflow.exam.mapper")
@EnableFeignClients
public class ExamServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamServiceApplication.class, args);
    }
}
