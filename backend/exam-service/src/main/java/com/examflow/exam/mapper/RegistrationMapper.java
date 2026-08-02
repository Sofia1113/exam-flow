package com.examflow.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.examflow.exam.entity.ExamRegistration;
import org.apache.ibatis.annotations.Mapper;

/**
 * 本地只读视图:直接查询共享库的 exam_registration(报名资格与场次校验)。
 * 分库迁移后改为 Feign 调用 registration-service。
 */
@Mapper
public interface RegistrationMapper extends BaseMapper<ExamRegistration> {
}
