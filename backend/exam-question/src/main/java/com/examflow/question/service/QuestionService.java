package com.examflow.question.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.examflow.common.context.UserContext;
import com.examflow.common.core.BusinessException;
import com.examflow.common.core.ErrorCode;
import com.examflow.common.core.PageResult;
import com.examflow.common.util.AesUtil;
import com.examflow.question.dto.ImportResult;
import com.examflow.question.dto.QuestionExcelRow;
import com.examflow.question.dto.QuestionVO;
import com.examflow.question.entity.Question;
import com.examflow.question.entity.QuestionKnowledge;
import com.examflow.question.entity.QuestionTag;
import com.examflow.question.entity.QuestionVersion;
import com.examflow.question.entity.SysSubject;
import com.examflow.question.mapper.QuestionKnowledgeMapper;
import com.examflow.question.mapper.QuestionMapper;
import com.examflow.question.mapper.QuestionTagMapper;
import com.examflow.question.mapper.QuestionVersionMapper;
import com.examflow.question.mapper.SubjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 题库服务:CRUD、7 类题型结构校验、答案加密存储、标签/知识点关联、修改留痕。
 *
 * <p>题型约定(答案结构与 PRD FR-QB-01 一致):
 * <ul>
 *   <li>single:options=JSON 数组(键唯一),answer=单个选项键,如 "A"</li>
 *   <li>multiple:options 同上,answer=键数组,如 ["A","C"]</li>
 *   <li>judge:answer="true"/"false",options 可空</li>
 *   <li>fill:answer=各空答案数组,如 ["答案一","答案二"]</li>
 *   <li>subjective/case/operation:answer=评分要点,无 options 约束</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionService {

    /** 权限码:查看(含答案)/编辑/审题(见 PermissionService)。 */
    public static final String PERM_VIEW = "question:view";
    public static final String PERM_EDIT = "question:edit";
    public static final String PERM_AUDIT = "question:audit";

    private final QuestionMapper questionMapper;
    private final QuestionTagMapper tagMapper;
    private final QuestionKnowledgeMapper knowledgeMapper;
    private final QuestionVersionMapper versionMapper;
    private final SubjectMapper subjectMapper;
    private final ObjectMapper objectMapper;
    private final PermissionService permissionService;

    public PageResult<QuestionVO> page(long page, long size, String type, Long subjectId,
                                       Integer difficulty, String status, String keyword) {
        LambdaQueryWrapper<Question> wrapper = Wrappers.lambdaQuery(Question.class);
        if (type != null && !type.isBlank()) {
            wrapper.eq(Question::getType, type);
        }
        if (subjectId != null) {
            wrapper.eq(Question::getSubjectId, subjectId);
        }
        if (difficulty != null) {
            wrapper.eq(Question::getDifficulty, difficulty);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(Question::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Question::getStem, keyword));
        }
        wrapper.orderByDesc(Question::getId);

        Page<Question> p = questionMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.convert(q -> toVO(q, false)));
    }

    /** 详情:解密答案与解析(供组卷/审题;权限控制待方法级鉴权落地)。 */
    public QuestionVO detail(Long id) {
        permissionService.requirePerm(PERM_VIEW);
        Question q = questionMapper.selectById(id);
        if (q == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "题目不存在");
        }
        return toVO(q, true);
    }

    @Transactional
    public Long create(QuestionReq req) {
        permissionService.requirePerm(PERM_EDIT);
        validateStructure(req);
        Question q = new Question();
        apply(q, req);
        q.setStatus("draft");
        q.setCreatorId(UserContext.requireUserId());
        questionMapper.insert(q);
        saveTagsAndKnowledges(q.getId(), req.tags(), req.knowledges());
        writeVersion(q.getId(), null, "create");
        log.info("创建题目: id={}, type={}", q.getId(), q.getType());
        return q.getId();
    }

    @Transactional
    public void update(Long id, QuestionReq req) {
        permissionService.requirePerm(PERM_EDIT);
        Question exist = questionMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "题目不存在");
        }
        if ("published".equals(exist.getStatus())) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "已发布题目不可直接修改,请先停用后重新送审");
        }
        validateStructure(req);
        String before = snapshotJson(exist);
        apply(exist, req);
        questionMapper.updateById(exist);
        saveTagsAndKnowledges(id, req.tags(), req.knowledges());
        writeVersion(id, before, "update");
        log.info("更新题目: id={}", id);
    }

    @Transactional
    public void delete(Long id) {
        permissionService.requirePerm(PERM_EDIT);
        Question exist = questionMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "题目不存在");
        }
        questionMapper.deleteById(id);
        log.info("删除题目: id={}", id);
    }

    // ---------- 审题流程(FR-QB-04):draft → pending → approved → published → disabled ----------

    /** 送审:仅草稿可送审。 */
    @Transactional
    public void submit(Long id) {
        permissionService.requirePerm(PERM_EDIT);
        Question q = require(id);
        if (!"draft".equals(q.getStatus())) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "仅草稿状态可送审");
        }
        q.setStatus("pending");
        questionMapper.updateById(q);
        writeVersion(id, snapshotJson(q), "submit");
        log.info("题目送审: id={}", id);
    }

    /** 审题:仅待审可审;出题人不得审核本人题目。 */
    @Transactional
    public void audit(Long id, boolean pass, String opinion) {
        permissionService.requirePerm(PERM_AUDIT);
        Question q = require(id);
        if (!"pending".equals(q.getStatus())) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "仅待审状态可审核");
        }
        Long operatorId = UserContext.requireUserId();
        Long authorId = findAuthor(id);
        if (authorId != null && authorId.equals(operatorId)) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "出题人不得审核本人题目");
        }
        q.setStatus(pass ? "approved" : "draft");
        questionMapper.updateById(q);
        writeVersion(id, snapshotJson(q), "audit");
        log.info("题目审核: id={}, pass={}, opinion={}", id, pass, opinion);
    }

    /** 发布:仅已审可发布。 */
    @Transactional
    public void publish(Long id) {
        permissionService.requirePerm(PERM_AUDIT);
        Question q = require(id);
        if (!"approved".equals(q.getStatus())) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "仅已审状态可发布");
        }
        q.setStatus("published");
        questionMapper.updateById(q);
        writeVersion(id, snapshotJson(q), "publish");
        log.info("题目发布: id={}", id);
    }

    /** 停用:仅已发布可停用;停用后可修改并重新送审。 */
    @Transactional
    public void disable(Long id) {
        permissionService.requirePerm(PERM_AUDIT);
        Question q = require(id);
        if (!"published".equals(q.getStatus())) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "仅已发布状态可停用");
        }
        q.setStatus("disabled");
        questionMapper.updateById(q);
        writeVersion(id, snapshotJson(q), "disable");
        log.info("题目停用: id={}", id);
    }

    // ---------- 批量导入导出(FR-QB-05) ----------

    /** 批量导入:逐行校验,错误行定位报告,成功行不受影响。 */
    public ImportResult importExcel(java.io.InputStream in) {
        permissionService.requirePerm(PERM_EDIT);
        List<QuestionExcelRow> rows = new ArrayList<>();
        EasyExcel.read(in, QuestionExcelRow.class, new ReadListener<QuestionExcelRow>() {
            @Override
            public void invoke(QuestionExcelRow row, AnalysisContext context) {
                rows.add(row);
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
            }
        }).sheet().doRead();

        List<ImportResult.RowError> errors = new ArrayList<>();
        int success = 0;
        for (int i = 0; i < rows.size(); i++) {
            int rowNo = i + 2; // 表头占第 1 行
            try {
                QuestionReq req = toReq(rows.get(i));
                validateStructure(req);
                Question q = new Question();
                apply(q, req);
                q.setStatus("draft");
                questionMapper.insert(q);
                saveTagsAndKnowledges(q.getId(), req.tags(), req.knowledges());
                writeVersion(q.getId(), null, "create");
                success++;
            } catch (BusinessException e) {
                errors.add(new ImportResult.RowError(rowNo, e.getMessage()));
            } catch (Exception e) {
                log.warn("导入第 {} 行失败", rowNo, e);
                errors.add(new ImportResult.RowError(rowNo, "数据格式不正确"));
            }
        }
        log.info("批量导入完成: total={}, success={}, fail={}", rows.size(), success, errors.size());
        return new ImportResult(rows.size(), success, errors.size(), errors);
    }

    /** 按过滤条件导出(含解密答案)。 */
    public void exportExcel(java.io.OutputStream out, String type, Long subjectId,
                            String status, String keyword) {
        permissionService.requirePerm(PERM_VIEW);
        LambdaQueryWrapper<Question> wrapper = Wrappers.lambdaQuery(Question.class);
        if (type != null && !type.isBlank()) {
            wrapper.eq(Question::getType, type);
        }
        if (subjectId != null) {
            wrapper.eq(Question::getSubjectId, subjectId);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(Question::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Question::getStem, keyword);
        }
        List<QuestionExcelRow> rows = questionMapper.selectList(wrapper).stream()
                .map(q -> toRow(q)).toList();
        EasyExcel.write(out, QuestionExcelRow.class).sheet("题目").doWrite(rows);
    }

    /** 下载导入模板。 */
    public void template(java.io.OutputStream out) {
        EasyExcel.write(out, QuestionExcelRow.class).sheet("题目模板").doWrite(List.of());
    }

    private QuestionExcelRow toRow(Question q) {
        QuestionExcelRow row = new QuestionExcelRow();
        row.setType(q.getType());
        row.setStem(q.getStem());
        row.setOptions(q.getOptions());
        row.setAnswer(decrypt(q.getAnswer()));
        row.setAnalysis(decrypt(q.getAnalysis()));
        row.setDifficulty(q.getDifficulty());
        row.setSubjectCode(q.getSubjectId() == null ? null
                : subjectMapper.selectById(q.getSubjectId()) == null ? null
                : subjectMapper.selectById(q.getSubjectId()).getCode());
        row.setTags(String.join(",", tagMapper.selectList(Wrappers.lambdaQuery(QuestionTag.class)
                .eq(QuestionTag::getQuestionId, q.getId()))
                .stream().map(QuestionTag::getTagName).toList()));
        row.setKnowledges(String.join(",", knowledgeMapper.selectList(Wrappers.lambdaQuery(QuestionKnowledge.class)
                .eq(QuestionKnowledge::getQuestionId, q.getId()))
                .stream().map(QuestionKnowledge::getKnowledgeName).toList()));
        return row;
    }

    private QuestionReq toReq(QuestionExcelRow row) {
        return new QuestionReq(row.getType(), row.getStem(), row.getOptions(), row.getAnswer(),
                row.getAnalysis(), row.getDifficulty(),
                row.getSubjectCode() == null ? null : subjectCodeToId(row.getSubjectCode()),
                null,
                row.getTags() == null ? List.of()
                        : java.util.Arrays.stream(row.getTags().split(",")).map(String::trim)
                        .filter(s -> !s.isBlank()).toList(),
                row.getKnowledges() == null ? List.of()
                        : java.util.Arrays.stream(row.getKnowledges().split(",")).map(String::trim)
                        .filter(s -> !s.isBlank()).toList());
    }

    private Long subjectCodeToId(String code) {
        var subject = subjectMapper.selectOne(Wrappers.lambdaQuery(SysSubject.class)
                .eq(SysSubject::getCode, code.trim()));
        if (subject == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "科目编码不存在: " + code);
        }
        return subject.getId();
    }

    private Question require(Long id) {
        Question q = questionMapper.selectById(id);
        if (q == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "题目不存在");
        }
        return q;
    }

    /** 出题人:题目最早 create 版本的操作人。 */
    private Long findAuthor(Long questionId) {
        List<QuestionVersion> versions = versionMapper.selectList(Wrappers.lambdaQuery(QuestionVersion.class)
                .eq(QuestionVersion::getQuestionId, questionId)
                .eq(QuestionVersion::getOperateType, "create")
                .orderByAsc(QuestionVersion::getId));
        return versions.isEmpty() ? null : versions.get(0).getOperatorId();
    }

    /** 题型结构校验(PRD FR-QB-02):不合规抛参数错误。 */
    private void validateStructure(QuestionReq req) {
        if (req.stem() == null || req.stem().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "题干不能为空");
        }
        if (req.subjectId() == null) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "必须指定科目");
        }
        try {
            switch (req.type()) {
                case "single" -> {
                    JsonNode options = parseOptions(req.options(), 2);
                    String answer = req.answer();
                    if (answer == null || !containsKey(options, answer)) {
                        throw new BusinessException(ErrorCode.VALIDATE_FAILED, "单选题答案必须是选项之一");
                    }
                }
                case "multiple" -> {
                    JsonNode options = parseOptions(req.options(), 2);
                    JsonNode answer = objectMapper.readTree(req.answer());
                    if (!answer.isArray() || answer.size() < 2) {
                        throw new BusinessException(ErrorCode.VALIDATE_FAILED, "多选题答案必须为 ≥2 个选项的数组");
                    }
                    Set<String> keys = new HashSet<>();
                    answer.forEach(a -> {
                        if (!containsKey(options, a.asText()) || !keys.add(a.asText())) {
                            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "多选题答案含非法或重复选项");
                        }
                    });
                }
                case "judge" -> {
                    if (!"true".equals(req.answer()) && !"false".equals(req.answer())) {
                        throw new BusinessException(ErrorCode.VALIDATE_FAILED, "判断题答案必须为 true/false");
                    }
                }
                case "fill" -> {
                    JsonNode answer = objectMapper.readTree(req.answer());
                    if (!answer.isArray() || answer.isEmpty()) {
                        throw new BusinessException(ErrorCode.VALIDATE_FAILED, "填空题答案必须为非空数组(每空一个)");
                    }
                }
                case "subjective", "case", "operation" -> {
                    if (req.answer() == null || req.answer().isBlank()) {
                        throw new BusinessException(ErrorCode.VALIDATE_FAILED, "主观题必须提供评分要点");
                    }
                }
                default -> throw new BusinessException(ErrorCode.VALIDATE_FAILED,
                        "未知题型: " + req.type());
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "答案/选项 JSON 格式不正确");
        }
    }

    /** 选项中是否存在指定键(数组节点不能用 has(key),须遍历)。 */
    private boolean containsKey(JsonNode options, String key) {
        for (JsonNode opt : options) {
            if (key.equals(opt.path("key").asText())) {
                return true;
            }
        }
        return false;
    }

    /** 解析选项并校验:JSON 数组且 ≥ minCount 项、键唯一。 */
    private JsonNode parseOptions(String options, int minCount) {
        try {
            JsonNode node = objectMapper.readTree(options);
            if (!node.isArray() || node.size() < minCount) {
                throw new BusinessException(ErrorCode.VALIDATE_FAILED,
                        "选项必须为 ≥" + minCount + " 项的 JSON 数组");
            }
            Set<String> keys = new HashSet<>();
            for (JsonNode opt : node) {
                String key = opt.path("key").asText();
                if (key.isBlank() || !keys.add(key)) {
                    throw new BusinessException(ErrorCode.VALIDATE_FAILED, "选项键缺失或重复");
                }
            }
            return node;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.VALIDATE_FAILED, "选项 JSON 格式不正确");
        }
    }

    private void apply(Question q, QuestionReq req) {
        q.setType(req.type());
        q.setStem(req.stem());
        q.setOptions(req.options());
        q.setAnswer(encrypt(req.answer()));
        q.setAnalysis(encrypt(req.analysis()));
        q.setDifficulty(req.difficulty() == null ? 3 : req.difficulty());
        q.setSubjectId(req.subjectId());
        q.setSource(req.source());
    }

    private String encrypt(String plain) {
        if (plain == null) {
            return null;
        }
        try {
            return AesUtil.encrypt(plain);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "答案加密失败");
        }
    }

    private String decrypt(String cipher) {
        if (cipher == null) {
            return null;
        }
        try {
            return AesUtil.decrypt(cipher);
        } catch (Exception e) {
            log.error("答案解密失败", e);
            return null;
        }
    }

    private void saveTagsAndKnowledges(Long questionId, List<String> tags, List<String> knowledges) {
        tagMapper.delete(Wrappers.lambdaQuery(QuestionTag.class).eq(QuestionTag::getQuestionId, questionId));
        knowledgeMapper.delete(Wrappers.lambdaQuery(QuestionKnowledge.class)
                .eq(QuestionKnowledge::getQuestionId, questionId));
        if (tags != null) {
            for (String tag : new HashSet<>(tags)) {
                QuestionTag t = new QuestionTag();
                t.setQuestionId(questionId);
                t.setTagName(tag);
                tagMapper.insert(t);
            }
        }
        if (knowledges != null) {
            for (String k : new HashSet<>(knowledges)) {
                QuestionKnowledge kq = new QuestionKnowledge();
                kq.setQuestionId(questionId);
                kq.setKnowledgeName(k);
                knowledgeMapper.insert(kq);
            }
        }
    }

    /** 修改留痕:写入变更前快照(question_version)。 */
    private void writeVersion(Long questionId, String before, String operateType) {
        QuestionVersion v = new QuestionVersion();
        v.setQuestionId(questionId);
        v.setContentSnapshot(before);
        v.setOperatorId(UserContext.currentUserId());
        v.setOperateType(operateType);
        v.setCreateTime(LocalDateTime.now());
        versionMapper.insert(v);
    }

    private String snapshotJson(Question q) {
        try {
            return objectMapper.writeValueAsString(q);
        } catch (Exception e) {
            return null;
        }
    }

    private QuestionVO toVO(Question q, boolean withSensitive) {
        List<String> tags = tagMapper.selectList(Wrappers.lambdaQuery(QuestionTag.class)
                        .eq(QuestionTag::getQuestionId, q.getId()))
                .stream().map(QuestionTag::getTagName).toList();
        List<String> knowledges = knowledgeMapper.selectList(Wrappers.lambdaQuery(QuestionKnowledge.class)
                        .eq(QuestionKnowledge::getQuestionId, q.getId()))
                .stream().map(QuestionKnowledge::getKnowledgeName).toList();
        String subjectName = q.getSubjectId() == null ? null
                : subjectMapper.selectById(q.getSubjectId()) == null ? null
                : subjectMapper.selectById(q.getSubjectId()).getName();
        return new QuestionVO(q.getId(), q.getType(), q.getStem(), q.getOptions(),
                withSensitive ? decrypt(q.getAnswer()) : null,
                withSensitive ? decrypt(q.getAnalysis()) : null,
                q.getDifficulty(), q.getSubjectId(), subjectName,
                q.getStatus(), q.getSource(), tags, knowledges, q.getCreateTime());
    }

    /** 题目创建/更新请求。 */
    public record QuestionReq(String type, String stem, String options, String answer,
                              String analysis, Integer difficulty, Long subjectId,
                              String source, List<String> tags, List<String> knowledges) {
    }
}
