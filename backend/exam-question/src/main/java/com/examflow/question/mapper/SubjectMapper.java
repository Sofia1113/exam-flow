package com.examflow.question.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.examflow.question.entity.SysSubject;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SubjectMapper extends BaseMapper<SysSubject> {
}
