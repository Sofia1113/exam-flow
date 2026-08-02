package com.examflow.grading.client;

import com.examflow.grading.dto.ExamSnapshot;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * paper-service 内部调用(判分需要快照标准答案,withAnswer=true)。
 */
@FeignClient(name = "exam-paper", url = "${examflow.paper-service-url:http://127.0.0.1:8084}")
public interface PaperServiceClient {

    @GetMapping("/internal/papers/{paperId}/exam-snapshot")
    ExamSnapshot examSnapshot(@PathVariable("paperId") Long paperId,
                              @RequestParam("withAnswer") boolean withAnswer);
}
