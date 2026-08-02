package com.examflow.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.examflow.exam.entity.ExamSlotView;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExamSlotMapper extends BaseMapper<ExamSlotView> {
}
