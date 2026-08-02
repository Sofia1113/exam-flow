package com.examflow.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.examflow.user.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoleMapper extends BaseMapper<SysRole> {
}
