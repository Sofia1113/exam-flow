package com.examflow.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.examflow.user.entity.SysOrg;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrgMapper extends BaseMapper<SysOrg> {
}
