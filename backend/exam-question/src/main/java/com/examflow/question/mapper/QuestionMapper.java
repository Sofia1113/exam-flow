package com.examflow.question.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.examflow.question.entity.Question;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {
}
