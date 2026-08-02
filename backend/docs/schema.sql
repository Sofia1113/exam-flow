-- ============================================================
-- exam-flow 在线考试系统 数据库结构(MySQL 8)
-- 对应文档:TDD.md §4.2 核心表设计(唯一权威来源,改动需同步更新文档)
-- 约定:
--   * 引擎 InnoDB,字符集 utf8mb4,排序 utf8mb4_unicode_ci
--   * 时间统一 DATETIME(UTC+8,应用层写入)
--   * 分数/金额统一 DECIMAL(8,2)
--   * 所有业务表含 id/create_time/update_time/deleted(逻辑删除);
--     audit_log 例外:只追加,禁止 update/delete
--   * 不建物理外键(政企惯例,一致性由应用层保证),仅建索引
--   * 加密字段(身份证/手机号/答案/答卷)由 AesUtil(AES-256-GCM)加密后存储,
--     类型统一 VARCHAR(2048)/LONGTEXT,应用层加解密
-- 分片说明:考试域 4 表(exam_session/answer_detail/exam_behavior_log/
--   exam_risk_record)由 ShardingSphere-JDBC 按注册/会话 ID 分片,
--   本脚本按单表结构给出,分片规则见 TDD §4.3
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `exam_flow`
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `exam_flow`;

-- ------------------------------------------------------------
-- 一、账号与组织(TDD §4.2.1)
-- ------------------------------------------------------------

-- 组织树:多级单位/部门,path 为层级路径(如 /1/2/3)
CREATE TABLE `sys_org` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `parent_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '父组织 ID,0 为根',
    `name`        VARCHAR(128) NOT NULL COMMENT '组织名称',
    `path`        VARCHAR(512) NOT NULL DEFAULT '' COMMENT '层级路径 /1/2/3',
    `org_type`    VARCHAR(32)  NOT NULL DEFAULT 'dept' COMMENT '类型:unit/dept/team',
    `status`      VARCHAR(16)  NOT NULL DEFAULT 'enabled' COMMENT 'enabled/disabled',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_parent` (`parent_id`)
) ENGINE = InnoDB COMMENT ='组织树';

-- 用户:身份证/手机号加密存储(phone/id_card 密文),脱敏展示
CREATE TABLE `sys_user` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `username`      VARCHAR(64)  NOT NULL COMMENT '登录账号',
    `password_hash` VARCHAR(128) NOT NULL COMMENT 'bcrypt 哈希(成本 12)',
    `name`          VARCHAR(64)  NOT NULL COMMENT '姓名',
    `phone`         VARCHAR(128) DEFAULT NULL COMMENT '手机号(AES 加密)',
    `id_card`       VARCHAR(128) DEFAULT NULL COMMENT '身份证号(AES 加密)',
    `org_id`        BIGINT       DEFAULT NULL COMMENT '所属组织',
    `user_type`     VARCHAR(16)  NOT NULL DEFAULT 'external' COMMENT 'internal=内部/external=社会考生',
    `status`        VARCHAR(16)  NOT NULL DEFAULT 'enabled' COMMENT 'enabled/disabled/locked',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_org` (`org_id`)
) ENGINE = InnoDB COMMENT ='用户(内部员工与社会考生)';

-- RBAC 基础四表
CREATE TABLE `sys_role` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `code`        VARCHAR(64)  NOT NULL COMMENT '角色编码,如 EXAM_ADMIN',
    `name`        VARCHAR(64)  NOT NULL COMMENT '角色名称',
    `remark`      VARCHAR(255) DEFAULT NULL,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE = InnoDB COMMENT ='角色';

CREATE TABLE `sys_user_role` (
    `id`      BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `role_id` BIGINT NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
) ENGINE = InnoDB COMMENT ='用户-角色';

CREATE TABLE `sys_role_perm` (
    `id`        BIGINT      NOT NULL AUTO_INCREMENT,
    `role_id`   BIGINT      NOT NULL,
    `perm_code` VARCHAR(128) NOT NULL COMMENT '权限编码(菜单/操作)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_perm` (`role_id`, `perm_code`)
) ENGINE = InnoDB COMMENT ='角色-权限';

-- 数据权限:角色 × 组织范围(全部/本级/下级),SQL 层拦截
CREATE TABLE `sys_data_scope` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `role_id`    BIGINT       NOT NULL,
    `scope_type` VARCHAR(16)  NOT NULL COMMENT 'all=全部/current=本级/children=本级及下级',
    `org_ids`    VARCHAR(512) DEFAULT NULL COMMENT '限定组织 ID 列表(JSON)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role` (`role_id`)
) ENGINE = InnoDB COMMENT ='数据权限范围';

-- 科目
CREATE TABLE `sys_subject` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `code`        VARCHAR(32)  NOT NULL COMMENT '科目编码',
    `name`        VARCHAR(128) NOT NULL COMMENT '科目名称',
    `status`      VARCHAR(16)  NOT NULL DEFAULT 'enabled',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE = InnoDB COMMENT ='科目';

-- 数据字典(考试类型/状态/异常类型等)
CREATE TABLE `sys_dict` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `dict_type`   VARCHAR(64)  NOT NULL COMMENT '字典类型',
    `dict_code`   VARCHAR(64)  NOT NULL COMMENT '字典项编码',
    `dict_label`  VARCHAR(128) NOT NULL COMMENT '字典项名称',
    `sort_no`     INT          NOT NULL DEFAULT 0,
    `status`      VARCHAR(16)  NOT NULL DEFAULT 'enabled',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type_code` (`dict_type`, `dict_code`)
) ENGINE = InnoDB COMMENT ='数据字典';

-- 全局参数(安全策略/防作弊阈值/公示期天数等)
CREATE TABLE `sys_param` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `param_key`   VARCHAR(64)  NOT NULL COMMENT '参数键,如 proctor.switch_limit',
    `param_value` VARCHAR(512) NOT NULL COMMENT '参数值',
    `param_desc`  VARCHAR(256) DEFAULT NULL,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_key` (`param_key`)
) ENGINE = InnoDB COMMENT ='全局参数';

-- ------------------------------------------------------------
-- 二、题库与试卷(TDD §4.2.2)
-- ------------------------------------------------------------

-- 题目:options/answer/analysis 为加密 JSON;题干富文本(LONGTEXT)
CREATE TABLE `question` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `type`        VARCHAR(16)  NOT NULL COMMENT 'single/multiple/judge/fill/subjective/case/operation',
    `stem`        LONGTEXT     NOT NULL COMMENT '题干(富文本,可含图片/LaTeX)',
    `options`     LONGTEXT     DEFAULT NULL COMMENT '选项(JSON,选择题)',
    `answer`      LONGTEXT     NOT NULL COMMENT '正确答案(AES 加密)',
    `analysis`    LONGTEXT     DEFAULT NULL COMMENT '答案解析(AES 加密)',
    `difficulty`  TINYINT      NOT NULL DEFAULT 3 COMMENT '难度 1-5',
    `subject_id`  BIGINT       NOT NULL COMMENT '科目 ID',
    `status`      VARCHAR(16)  NOT NULL DEFAULT 'draft' COMMENT 'draft/pending/approved/published/disabled',
    `source`      VARCHAR(64)  DEFAULT NULL COMMENT '题目来源',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_subject_status` (`subject_id`, `status`),
    KEY `idx_type` (`type`)
) ENGINE = InnoDB COMMENT ='题目';

-- 题目历史版本:修改留痕,审计可追溯
CREATE TABLE `question_version` (
    `id`               BIGINT   NOT NULL AUTO_INCREMENT,
    `question_id`      BIGINT   NOT NULL,
    `content_snapshot` LONGTEXT NOT NULL COMMENT '变更前内容(JSON)',
    `operator_id`      BIGINT   NOT NULL,
    `operate_type`     VARCHAR(32) NOT NULL COMMENT 'create/update/audit/disable',
    `create_time`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_question` (`question_id`)
) ENGINE = InnoDB COMMENT ='题目版本历史';

-- 题目标签与知识点
CREATE TABLE `question_tag` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT,
    `question_id` BIGINT      NOT NULL,
    `tag_name`    VARCHAR(64) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_question_tag` (`question_id`, `tag_name`)
) ENGINE = InnoDB COMMENT ='题目标签';

CREATE TABLE `question_knowledge` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT,
    `question_id`   BIGINT      NOT NULL,
    `knowledge_name` VARCHAR(64) NOT NULL COMMENT '知识点名称(组卷按知识点抽题)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_question_knowledge` (`question_id`, `knowledge_name`),
    KEY `idx_knowledge` (`knowledge_name`)
) ENGINE = InnoDB COMMENT ='题目知识点';

-- 试卷:blueprint 为组卷蓝图(JSON);状态机 draft/pending/approved/published/archived
CREATE TABLE `paper` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT,
    `name`        VARCHAR(128)  NOT NULL COMMENT '试卷名称',
    `subject_id`  BIGINT        NOT NULL,
    `total_score` DECIMAL(8, 2) NOT NULL DEFAULT 100.00,
    `pass_score`  DECIMAL(8, 2) NOT NULL DEFAULT 60.00 COMMENT '及格线',
    `duration_min` INT          NOT NULL DEFAULT 120 COMMENT '考试时长(分钟)',
    `paper_type`  VARCHAR(16)   NOT NULL DEFAULT 'fixed' COMMENT 'fixed=固定/strategy=策略',
    `blueprint`   LONGTEXT      DEFAULT NULL COMMENT '组卷蓝图(JSON,策略卷)',
    `status`      VARCHAR(16)   NOT NULL DEFAULT 'draft',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_subject` (`subject_id`)
) ENGINE = InnoDB COMMENT ='试卷';

-- 试卷快照:发布即固化(不可变),content 为整卷加密 JSON;
-- 题目后续修改不影响已发布考试,申诉时可完整还原
CREATE TABLE `paper_snapshot` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT,
    `paper_id`    BIGINT        NOT NULL,
    `snapshot_no` VARCHAR(32)   NOT NULL COMMENT '快照编号,如 SNAP-20260801-001',
    `content`     LONGTEXT      NOT NULL COMMENT '整卷内容(AES 加密 JSON,含题目内容快照)',
    `total_score` DECIMAL(8, 2) NOT NULL DEFAULT 100.00,
    `version`     INT           NOT NULL DEFAULT 1,
    `status`      VARCHAR(16)   NOT NULL DEFAULT 'active' COMMENT 'active/archived',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_snapshot_no` (`snapshot_no`),
    KEY `idx_paper` (`paper_id`)
) ENGINE = InnoDB COMMENT ='试卷快照(不可变)';

-- 快照题目明细:题目内容快照而非引用题 ID,支持确定性选项乱序(shuffle_group)
CREATE TABLE `paper_question` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT,
    `snapshot_id`       BIGINT        NOT NULL,
    `question_snapshot` LONGTEXT      NOT NULL COMMENT '题目内容快照(AES 加密 JSON)',
    `seq`               INT           NOT NULL COMMENT '卷面序号',
    `score`             DECIMAL(8, 2) NOT NULL COMMENT '本题分值',
    `shuffle_group`     INT           DEFAULT 0 COMMENT '乱序组:同组题目/选项可随机打乱,0=固定',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_snapshot_seq` (`snapshot_id`, `seq`),
    KEY `idx_snapshot` (`snapshot_id`)
) ENGINE = InnoDB COMMENT ='试卷快照题目明细';

-- ------------------------------------------------------------
-- 三、报名与排考(TDD §4.2.3)
-- ------------------------------------------------------------

-- 考试计划:报名条件规则 condition_rule(JSON);状态机 draft/pending/approved/running/closed
CREATE TABLE `exam_plan` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT,
    `name`            VARCHAR(128)  NOT NULL COMMENT '考试名称',
    `subject_id`      BIGINT        NOT NULL,
    `paper_id`        BIGINT        DEFAULT NULL COMMENT '试卷(审批通过后关联)',
    `reg_start`       DATETIME      NOT NULL COMMENT '报名开始',
    `reg_end`         DATETIME      NOT NULL COMMENT '报名截止',
    `exam_date`       DATE          NOT NULL,
    `capacity`        INT           NOT NULL DEFAULT 0 COMMENT '名额上限,0=不限',
    `condition_rule`  LONGTEXT      DEFAULT NULL COMMENT '报名条件规则(JSON:组织范围/工龄/前置资格)',
    `status`          VARCHAR(16)   NOT NULL DEFAULT 'draft',
    `approver_id`     BIGINT        DEFAULT NULL,
    `approve_time`    DATETIME      DEFAULT NULL,
    `approve_opinion` VARCHAR(512)  DEFAULT NULL,
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB COMMENT ='考试计划';

-- 报名记录:uk(plan_id,user_id) 防重复报名,并发抢报由唯一索引兜底
CREATE TABLE `exam_registration` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `plan_id`        BIGINT       NOT NULL,
    `user_id`        BIGINT       NOT NULL,
    `slot_id`        BIGINT       DEFAULT NULL COMMENT '场次(排考后回填)',
    `status`         VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected/withdrawn',
    `audit_by`       BIGINT       DEFAULT NULL,
    `audit_opinion`  VARCHAR(512) DEFAULT NULL,
    `audit_time`     DATETIME     DEFAULT NULL,
    `ticket_no`      VARCHAR(32)  DEFAULT NULL COMMENT '准考证号(审核通过后生成)',
    `waitlist`       TINYINT      NOT NULL DEFAULT 0 COMMENT '1=候补',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_plan_user` (`plan_id`, `user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_ticket` (`ticket_no`)
) ENGINE = InnoDB COMMENT ='考试报名记录';

-- 场次:一个考次可分多场错峰;proctor_ids 为监考员 ID 列表(JSON)
CREATE TABLE `exam_slot` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `plan_id`     BIGINT       NOT NULL,
    `slot_name`   VARCHAR(64)  NOT NULL COMMENT '场次名称,如 上午场 09:00-11:00',
    `start_time`  DATETIME     NOT NULL,
    `end_time`    DATETIME     NOT NULL,
    `capacity`    INT          NOT NULL DEFAULT 0,
    `seat_count`  INT          NOT NULL DEFAULT 0 COMMENT '已分配机位',
    `proctor_ids` VARCHAR(512) DEFAULT NULL COMMENT '监考员 ID 列表(JSON)',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_plan` (`plan_id`)
) ENGINE = InnoDB COMMENT ='考试场次';

-- ------------------------------------------------------------
-- 四、考试会话与作答(TDD §4.2.4)★ 考试域分片表
-- 分片键:exam_session/answer_detail/exam_behavior_log/exam_risk_record
--         按 registration_id/session_id 一致性哈希,16 库 × 64 表
-- ------------------------------------------------------------

-- 考试会话:seed 保证抽题可复现(审计重放);question_ids 为抽题结果(加密 JSON)
CREATE TABLE `exam_session` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT,
    `session_no`         VARCHAR(64)  NOT NULL COMMENT '会话号',
    `registration_id`    BIGINT       NOT NULL COMMENT '报名记录 ID(分片键)',
    `slot_id`            BIGINT       NOT NULL,
    `paper_snapshot_id`  BIGINT       NOT NULL,
    `seed`               VARCHAR(64)  NOT NULL COMMENT '抽题种子 SHA256(snapshot+slot+candidate)',
    `question_ids`       LONGTEXT     NOT NULL COMMENT '抽题结果题目序列(加密 JSON)',
    `status`             VARCHAR(16)  NOT NULL DEFAULT 'ANSWERING' COMMENT 'ANSWERING/SUBMITTED/GRADING/GRADED/CLOSED/VOID',
    `started_at`         DATETIME     NOT NULL COMMENT '作答开始(服务器时间)',
    `deadline_at`        DATETIME     NOT NULL COMMENT '截止时间(服务器时间,客户端不可伪造)',
    `enter_count`        INT          NOT NULL DEFAULT 0 COMMENT '已进入次数(上限可配,默认 3)',
    `last_seq`           BIGINT       NOT NULL DEFAULT 0 COMMENT '服务端已确认作答序号',
    `submit_time`        DATETIME     DEFAULT NULL,
    `client_ip`          VARCHAR(64)  DEFAULT NULL,
    `device_fp`          VARCHAR(128) DEFAULT NULL COMMENT '设备指纹',
    `create_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`            TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_registration` (`registration_id`) COMMENT '一名考生一考次仅一个会话',
    UNIQUE KEY `uk_session_no` (`session_no`),
    KEY `idx_slot_status` (`slot_id`, `status`)
) ENGINE = InnoDB COMMENT ='考试会话(分片表,分片键=registration_id)';

-- 作答明细:answer 为加密 JSON;version 支持 seq 对齐(幂等 upsert)
CREATE TABLE `answer_detail` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT,
    `session_id`   BIGINT        NOT NULL COMMENT '会话 ID(分片键)',
    `question_seq` INT           NOT NULL COMMENT '卷面题目序号',
    `answer`       LONGTEXT      NOT NULL COMMENT '作答内容(AES 加密 JSON,附件为对象存储地址)',
    `score`        DECIMAL(8, 2) DEFAULT NULL COMMENT '得分(判分后回填)',
    `score_status` VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT 'pending/graded',
    `version`      INT           NOT NULL DEFAULT 0 COMMENT '版本号(seq 对齐用)',
    `update_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_session_question` (`session_id`, `question_seq`),
    KEY `idx_session` (`session_id`)
) ENGINE = InnoDB COMMENT ='作答明细(分片表,分片键=session_id,无逻辑删除)';

-- ------------------------------------------------------------
-- 五、监考与评阅(TDD §4.2.5)
-- ------------------------------------------------------------

-- 考试行为日志:切屏/离屏/粘贴/失焦/抓拍/IP 变化,供风险引擎与监察取证
CREATE TABLE `exam_behavior_log` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `session_id`  BIGINT       NOT NULL COMMENT '会话 ID(分片键)',
    `action`      VARCHAR(32)  NOT NULL COMMENT 'switch/leave/paste/blur/capture/ip_change',
    `occur_time`  DATETIME     NOT NULL,
    `meta`        LONGTEXT     DEFAULT NULL COMMENT '事件元数据(JSON,如切屏时长/目标 URL)',
    `risk_level`  TINYINT      NOT NULL DEFAULT 0 COMMENT '0 正常/1 警告/2 疑似作弊',
    PRIMARY KEY (`id`),
    KEY `idx_session_time` (`session_id`, `occur_time`)
) ENGINE = InnoDB COMMENT ='考试行为日志(分片表,分片键=session_id,只追加)';

-- 风险记录:切屏计数/告警/处置/申诉闭环(≤3 个工作日)
CREATE TABLE `exam_risk_record` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `session_id`     BIGINT       NOT NULL COMMENT '会话 ID(分片键)',
    `risk_type`      VARCHAR(32)  NOT NULL COMMENT 'switch_exceed/paste/ip_change/face_fail',
    `count`          INT          NOT NULL DEFAULT 0 COMMENT '累计次数',
    `status`         VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT 'pending/handled/appealed/resolved',
    `handle_by`      BIGINT       DEFAULT NULL,
    `handle_opinion` VARCHAR(512) DEFAULT NULL,
    `handle_time`    DATETIME     DEFAULT NULL,
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_session` (`session_id`)
) ENGINE = InnoDB COMMENT ='考试风险记录(分片表,分片键=session_id)';

-- 评阅任务:一轮主观题一个任务(一评/二评/仲裁轮次流转)
CREATE TABLE `grading_task` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `session_id`  BIGINT       NOT NULL,
    `question_seq` INT         NOT NULL COMMENT '卷面题目序号',
    `grader_ids`  VARCHAR(256) DEFAULT NULL COMMENT '各轮次阅卷员 ID(JSON,含仲裁)',
    `round`       VARCHAR(16)  NOT NULL DEFAULT 'first' COMMENT 'first/second/arbitration',
    `status`      VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT 'pending/grading/graded/arbitrated',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_session_question` (`session_id`, `question_seq`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB COMMENT ='主观题评阅任务';

-- 评分记录:双评/仲裁每次评分一条;uk(task,grader) 防重复提交
CREATE TABLE `grading_record` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT,
    `task_id`     BIGINT        NOT NULL,
    `grader_id`   BIGINT        NOT NULL,
    `score`       DECIMAL(8, 2) NOT NULL,
    `comment`     VARCHAR(1024) DEFAULT NULL,
    `submit_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_grader` (`task_id`, `grader_id`)
) ENGINE = InnoDB COMMENT ='评分记录';

-- ------------------------------------------------------------
-- 六、成绩与审计(TDD §4.2.6)
-- ------------------------------------------------------------

-- 成绩:发布状态机 unpublished/publicity/published
CREATE TABLE `score_record` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT,
    `session_id`       BIGINT        NOT NULL,
    `total_score`      DECIMAL(8, 2) NOT NULL DEFAULT 0.00,
    `objective_score`  DECIMAL(8, 2) NOT NULL DEFAULT 0.00 COMMENT '客观题得分',
    `subjective_score` DECIMAL(8, 2) NOT NULL DEFAULT 0.00 COMMENT '主观题得分',
    `pass_flag`        TINYINT       NOT NULL DEFAULT 0 COMMENT '1=合格',
    `publish_status`   VARCHAR(16)   NOT NULL DEFAULT 'unpublished' COMMENT 'unpublished/publicity/published',
    `publicity_start`  DATETIME      DEFAULT NULL COMMENT '公示期开始',
    `publicity_end`    DATETIME      DEFAULT NULL COMMENT '公示期结束',
    `create_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_session` (`session_id`)
) ENGINE = InnoDB COMMENT ='成绩记录';

-- 成绩更正:申请→复核→审批→发布,全程留痕(发布后禁直接改 score_record)
CREATE TABLE `score_correction` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT,
    `score_id`    BIGINT        NOT NULL,
    `from_value`  DECIMAL(8, 2) NOT NULL,
    `to_value`    DECIMAL(8, 2) NOT NULL,
    `reason`      VARCHAR(512)  NOT NULL,
    `applicant`   BIGINT        NOT NULL,
    `approver`    BIGINT        DEFAULT NULL,
    `approve_time` DATETIME     DEFAULT NULL,
    `status`      VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_score` (`score_id`)
) ENGINE = InnoDB COMMENT ='成绩更正记录';

-- 审计日志:★ 只追加,禁止 update/delete;独立存储,留存 ≥ 5 年;
-- 生产环境授予 INSERT/SELECT 权限并移除 DML 权限
CREATE TABLE `audit_log` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `operator_id` BIGINT       DEFAULT NULL COMMENT '操作人(anonymous 表示未登录)',
    `action`      VARCHAR(64)  NOT NULL COMMENT '动作,如 交卷/成绩更正/强制交卷',
    `module`      VARCHAR(32)  NOT NULL COMMENT '模块:auth/exam/paper/score...',
    `object_type` VARCHAR(32)  DEFAULT NULL COMMENT '对象类型:session/score/paper',
    `object_id`   VARCHAR(64)  DEFAULT NULL COMMENT '对象 ID',
    `before_data` LONGTEXT     DEFAULT NULL COMMENT '变更前(JSON)',
    `after_data`  LONGTEXT     DEFAULT NULL COMMENT '变更后(JSON)',
    `ip`          VARCHAR(64)  DEFAULT NULL,
    `success`     TINYINT      NOT NULL DEFAULT 1,
    `trace_id`    VARCHAR(64)  DEFAULT NULL,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_operator_time` (`operator_id`, `create_time`),
    KEY `idx_module_time` (`module`, `create_time`)
) ENGINE = InnoDB COMMENT ='审计日志(只追加,禁改禁删)';

-- 通知记录:渠道/模板/回执,失败重试 3 次
CREATE TABLE `notify_record` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `channel`       VARCHAR(16)  NOT NULL COMMENT 'sms/site/email/wechat',
    `template_code` VARCHAR(64)  NOT NULL,
    `target`        VARCHAR(128) NOT NULL COMMENT '手机号/用户 ID/邮箱',
    `content`       LONGTEXT     DEFAULT NULL COMMENT '渲染后内容',
    `status`        VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT 'pending/success/failed',
    `retry_count`   INT          NOT NULL DEFAULT 0,
    `receipt`       VARCHAR(128) DEFAULT NULL COMMENT '通道回执',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_target` (`target`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB COMMENT ='通知记录';

SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------------------------------------------
-- 初始化数据(最小可用):管理员账号 / 基础角色 / 关键参数
-- 说明:管理员初始密码 admin123456(首次登录后必须修改,生产环境由初始化流程注入随机密码)
-- ------------------------------------------------------------

INSERT INTO `sys_org` (`id`, `parent_id`, `name`, `path`, `org_type`) VALUES
(1, 0, '考试管理委员会', '/1', 'unit');

-- 密码为 bcrypt(admin123456,$2y$ 前缀与 $2a$ 兼容)
INSERT INTO `sys_user` (`id`, `username`, `password_hash`, `name`, `org_id`, `user_type`, `status`) VALUES
(1, 'admin', '$2y$12$v2j6/D/qf3uFN1XPTCVW1O5ruSnpltkzBFCVWWfnkOUrmskC9IYH6', '系统管理员', 1, 'internal', 'enabled');

INSERT INTO `sys_role` (`id`, `code`, `name`, `remark`) VALUES
(1, 'SYS_ADMIN', '系统管理员', '全部权限'),
(2, 'EXAM_ADMIN', '考试管理员', '考试全流程管理'),
(3, 'CANDIDATE', '考生', '报名与考试');

INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);

INSERT INTO `sys_data_scope` (`role_id`, `scope_type`) VALUES (1, 'all');

INSERT INTO `sys_param` (`param_key`, `param_value`, `param_desc`) VALUES
('proctor.switch_limit', '3', '切屏告警阈值(次),超限标记疑似作弊'),
('proctor.force_submit_limit', '6', '切屏强制交卷阈值(次)'),
('score.publicity_days', '3', '成绩公示期(天)'),
('exam.max_enter_count', '3', '单场考试允许进入次数'),
('exam.late_minutes', '30', '开考后禁止入场分钟数');

INSERT INTO `sys_dict` (`dict_type`, `dict_code`, `dict_label`, `sort_no`) VALUES
('exam_plan_status', 'draft', '草稿', 1),
('exam_plan_status', 'pending', '待审批', 2),
('exam_plan_status', 'approved', '已审批', 3),
('exam_plan_status', 'running', '进行中', 4),
('exam_plan_status', 'closed', '已结束', 5),
('reg_status', 'pending', '待审核', 1),
('reg_status', 'approved', '已通过', 2),
('reg_status', 'rejected', '已驳回', 3),
('reg_status', 'withdrawn', '已退考', 4);
