# exam-flow 在线考试系统

考试流程管理系统(政企风格):覆盖题库 → 组卷 → 报名审核 → 排考 → 在线考试 → 监考防作弊 → 阅卷 → 成绩发布 → 证书归档 → 数据分析全流程。

## 文档

| 文档 | 说明 |
|---|---|
| [PRD.md](./PRD.md) | 产品需求文档 v0.1 |
| [TDD.md](./TDD.md) | 技术设计文档 v0.1 |
| [DESIGN.md](./DESIGN.md) | 视觉设计规范(政企风格,所有 UI 必须遵循) |
| [CLAUDE.md](./CLAUDE.md) | 项目规则 |

## 工程结构

```
exam-flow/
├─ backend/            # Java 17 + Spring Boot 3,13 个 Maven 模块
│  ├─ exam-common/     # 公共库:统一响应/错误码/加密/审计切面
│  ├─ exam-gateway/    # API 网关(路由/鉴权/防重放)
│  └─ exam-*/          # 10 个业务服务(端口 8081-8091)
├─ frontend/           # Vue3 + TS + Vite
│  ├─ portal/          # 考生门户(5173)
│  ├─ admin/           # 管理后台(5174)
│  └─ exam-client/     # 在线考试端,全屏应用(5175)
└─ deploy/             # docker-compose/nginx/k8s/运维脚本
```

## 快速开始(本地联调)

```bash
# 1. 基础设施(MySQL/Redis/Nacos/RocketMQ/MinIO)
docker compose -f deploy/docker/docker-compose.yml up -d

# 2. 后端(示例启动认证服务与考试服务)
cd backend
mvn -pl exam-auth -am spring-boot:run        # 8081
mvn -pl exam-service -am spring-boot:run     # 8086

# 3. 前端
cd frontend/portal && npm install && npm run dev      # http://localhost:5173
cd frontend/admin && npm install && npm run dev       # http://localhost:5174
cd frontend/exam-client && npm install && npm run dev # http://localhost:5175
```

> 当前为骨架版本:接口返回统一结构但业务逻辑为占位(TODO 标注),可按 TDD §12 规范逐步实现。
> 数据库 DDL 按 [TDD §4.2](./TDD.md#42-核心表设计) 生成(待交付)。

## 技术栈

Java 17 · Spring Boot 3 · Spring Cloud Alibaba(Nacos/Sentinel)· MySQL 8 · Redis · RocketMQ · Vue 3 · TypeScript · Vite · Docker/K8s

详细设计见 [TDD.md](./TDD.md)。
