package com.examflow.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.examflow.user.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表 Mapper。
 */
@Mapper
public interface UserMapper extends BaseMapper<SysUser> {
}
