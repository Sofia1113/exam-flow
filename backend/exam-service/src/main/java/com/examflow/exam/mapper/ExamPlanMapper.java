package com.examflow.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.examflow.exam.entity.ExamPlanView;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExamPlanMapper extends BaseMapper<ExamPlanView> {
}
