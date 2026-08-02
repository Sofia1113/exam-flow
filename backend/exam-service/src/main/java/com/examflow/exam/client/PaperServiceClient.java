package com.examflow.exam.client;

import com.examflow.exam.dto.ExamSnapshot;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * paper-service 内部调用(考试快照,无答案;判分场景 withAnswer=true)。
 */
@FeignClient(name = "exam-paper", url = "${examflow.paper-service-url:http://127.0.0.1:8084}")
public interface PaperServiceClient {

    @GetMapping("/internal/papers/{paperId}/exam-snapshot")
    ExamSnapshot examSnapshot(@PathVariable("paperId") Long paperId,
                              @RequestParam("withAnswer") boolean withAnswer);
}
