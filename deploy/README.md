# deploy 部署目录说明

| 目录 | 内容 | 说明 |
|---|---|---|
| `docker/` | docker-compose.yml(基础设施)、Dockerfile(后端通用镜像) | 本地/测试环境联调;生产用 K8s |
| `nginx/` | nginx.conf(HTTPS 反代入口) | 接入与安全层:WAF → Nginx → 网关 |
| `k8s/` | 核心服务清单示例(考试服务 Deployment/Service/HPA) | 双可用区部署,见 `k8s/README.md` |
| `scripts/` | backup.sh(备份)、pre-exam-check.sh(考前巡检) | 生产运维脚本 |

## 本地联调快速开始

```bash
# 1. 启动基础设施
docker compose -f docker/docker-compose.yml up -d

# 2. 初始化数据库(按 TDD §4.2 建表,DDL 脚本待生成)
#    mysql -h127.0.0.1 -uroot -proot exam_flow < backend/docs/schema.sql

# 3. 启动后端(以认证服务为例)
cd backend && mvn -pl exam-auth -am spring-boot:run

# 4. 启动前端
cd frontend/portal && npm install && npm run dev
```

## 生产部署要求(TDD §9)

- 双可用区 K8s,核心服务 3 副本跨区,`maxUnavailable: 0`;
- MySQL MGR 强同步 + 跨区异步从库;Redis Cluster 3 主 3 从;
- 每日全量 + binlog 增量备份(RPO ≤ 5 分钟),季度恢复演练;
- 考前 2 小时至考后变更冻结,运行 `scripts/pre-exam-check.sh` 巡检。
