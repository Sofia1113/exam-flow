package com.examflow.user.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.examflow.common.util.AesUtil;
import com.examflow.user.dto.UserInfo;
import com.examflow.user.entity.SysUser;
import com.examflow.user.mapper.UserMapper;
import com.examflow.user.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务间内部接口(/internal/** 不经过网关路由,仅服务直连)。
 * 注意:返回含 passwordHash 与权限码,仅限可信服务调用,后续接入服务间调用鉴权(X-001)。
 */
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserMapper userMapper;
    private final UserService userService;

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

    /** 指定用户的有效权限码集合(SYS_ADMIN 返回 ["*"],供各服务方法级授权校验)。 */
    @GetMapping("/{id}/perms")
    public List<String> getPerms(@PathVariable Long id) {
        return userService.getUserPerms(id);
    }

    /** 按手机号查询(密文匹配);不存在返回 null。 */
    @GetMapping("/by-phone")
    public UserInfo getByPhone(@RequestParam String phone) {
        SysUser user = userMapper.selectOne(Wrappers.lambdaQuery(SysUser.class)
                .eq(SysUser::getPhone, encrypt(phone)));
        if (user == null) {
            return null;
        }
        return toInfo(user);
    }

    /** 短信验证码自动注册(external 考生)。 */
    @PostMapping("/register")
    public Long register(@RequestBody RegisterReq req) {
        return userService.autoRegister(req.phone());
    }

    private UserInfo toInfo(SysUser user) {
        return new UserInfo(user.getId(), user.getUsername(), user.getName(),
                user.getPasswordHash(), user.getStatus(), user.getUserType(), user.getOrgId());
    }

    private String encrypt(String plain) {
        try {
            return AesUtil.encrypt(plain);
        } catch (Exception e) {
            throw new IllegalStateException("手机号加密失败", e);
        }
    }

    public record RegisterReq(String phone) {
    }
}
