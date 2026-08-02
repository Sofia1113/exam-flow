package com.examflow.paper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.examflow.paper.entity.Paper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaperMapper extends BaseMapper<Paper> {
}
