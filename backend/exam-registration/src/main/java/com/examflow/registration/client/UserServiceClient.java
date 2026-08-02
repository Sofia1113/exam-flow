package com.examflow.registration.client;

import com.examflow.registration.dto.UserInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * user-service 内部调用客户端(报名资格校验需要用户组织信息)。
 */
@FeignClient(name = "exam-user", url = "${examflow.user-service-url:http://127.0.0.1:8082}")
public interface UserServiceClient {

    @GetMapping("/internal/users/{id}")
    UserInfo getUser(@PathVariable("id") Long userId);
}
