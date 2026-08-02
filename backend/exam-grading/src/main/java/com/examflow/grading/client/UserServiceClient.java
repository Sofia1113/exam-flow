package com.examflow.grading.client;

import com.examflow.grading.dto.UserInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * user-service 内部调用(成绩导出需要考生姓名)。
 */
@FeignClient(name = "exam-user", url = "${examflow.user-service-url:http://127.0.0.1:8082}")
public interface UserServiceClient {

    @GetMapping("/internal/users/{id}")
    UserInfo getUser(@PathVariable("id") Long userId);
}
