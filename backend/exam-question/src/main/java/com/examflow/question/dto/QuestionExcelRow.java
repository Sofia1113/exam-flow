package com.examflow.question.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 题目 Excel 导入导出行(与模板列一一对应)。
 * options/answer 为 JSON 字符串;answer 导出时解密为明文。
 */
@Data
public class QuestionExcelRow {

    @ExcelProperty("题型")
    private String type;

    @ExcelProperty("题干")
    private String stem;

    @ExcelProperty("选项(JSON)")
    private String options;

    @ExcelProperty("答案")
    private String answer;

    @ExcelProperty("解析")
    private String analysis;

    @ExcelProperty("难度(1-5)")
    private Integer difficulty;

    @ExcelProperty("科目编码")
    private String subjectCode;

    @ExcelProperty("标签(逗号分隔)")
    private String tags;

    @ExcelProperty("知识点(逗号分隔)")
    private String knowledges;
}
