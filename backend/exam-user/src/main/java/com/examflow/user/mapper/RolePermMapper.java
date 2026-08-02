package com.examflow.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.examflow.user.entity.SysRolePerm;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RolePermMapper extends BaseMapper<SysRolePerm> {
}
