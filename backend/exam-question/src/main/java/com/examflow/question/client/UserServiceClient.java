package com.examflow.question.client;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * user-service 内部调用客户端(方法级授权,见 TDD §7.3)。
 * 生产环境:注册 Nacos 后改为 name = "exam-user"(去掉 url)。
 */
@FeignClient(name = "exam-user", url = "${examflow.user-service-url:http://127.0.0.1:8082}")
public interface UserServiceClient {

    /** 指定用户的权限码集合(SYS_ADMIN 返回 ["*"])。 */
    @GetMapping("/internal/users/{id}/perms")
    List<String> getPerms(@PathVariable("id") Long userId);
}
