package com.examflow.user.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.examflow.user.dto.UserInfo;
import com.examflow.user.entity.SysUser;
import com.examflow.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务间内部接口(/internal/** 不经过网关路由,仅服务直连)。
 * 注意:返回含 passwordHash,仅认证服务可调用,后续接入服务间调用鉴权(X-001)。
 */
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserMapper userMapper;

    /** 按登录账号查询用户;不存在返回 null(调用方判空)。 */
    @GetMapping("/by-username")
    public UserInfo getByUsername(@RequestParam String username) {
        SysUser user = userMapper.selectOne(Wrappers.lambdaQuery(SysUser.class)
                .eq(SysUser::getUsername, username));
        if (user == null) {
            return null;
        }
        return new UserInfo(user.getId(), user.getUsername(), user.getName(),
                user.getPasswordHash(), user.getStatus(), user.getUserType(), user.getOrgId());
    }
}
