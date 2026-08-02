package com.examflow.auth.client;

import com.examflow.auth.dto.UserInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * user-service 内部调用客户端。
 * 生产环境:注册 Nacos 后改为 name = "exam-user"(去掉 url)走服务发现;
 * 内部接口 /internal/** 不经过网关路由,仅服务间直连。
 */
@FeignClient(name = "exam-user", url = "${examflow.user-service-url:http://127.0.0.1:8082}")
public interface UserServiceClient {

    /** 按登录账号查询用户(含口令哈希,仅供认证服务使用)。 */
    @GetMapping("/internal/users/by-username")
    UserInfo getByUsername(@RequestParam("username") String username);
}
