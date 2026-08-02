package com.examflow.paper.client;

import com.examflow.paper.dto.QuestionSnapshot;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * question-service 内部调用客户端(组卷拉取题目快照/候选题池)。
 * 生产环境:注册 Nacos 后改为 name = "exam-question"(去掉 url)。
 */
@FeignClient(name = "exam-question", url = "${examflow.question-service-url:http://127.0.0.1:8083}")
public interface QuestionServiceClient {

    /** 按 ID 批量取题目快照(固定组卷/快照用)。 */
    @GetMapping("/internal/questions/batch")
    List<QuestionSnapshot> batch(@RequestParam("ids") List<Long> ids);

    /** 候选题池:已发布题目(策略组卷)。 */
    @GetMapping("/internal/questions/pool")
    List<QuestionSnapshot> pool(@RequestParam("subjectId") Long subjectId,
                                @RequestParam(value = "type", required = false) String type,
                                @RequestParam(value = "difficulty", required = false) Integer difficulty,
                                @RequestParam(value = "knowledge", required = false) String knowledge);
}
