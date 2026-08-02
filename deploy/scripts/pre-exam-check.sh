#!/usr/bin/env bash
# ============================================================
# exam-flow 考前自动化巡检脚本(骨架,TDD §8.3)
# 开考前运行,任一必检项失败应阻塞考试开启。
# 用法:pre-exam-check.sh
# ============================================================
set -uo pipefail

FAILED=0

check_port() {
  # $1 名称  $2 host  $3 port
  if nc -z -w 3 "$2" "$3" 2>/dev/null; then
    echo "[OK] $1 ($2:$3)"
  else
    echo "[FAIL] $1 不可达 ($2:$3)"
    FAILED=1
  fi
}

check_http() {
  # $1 名称  $2 url
  if curl -sf --max-time 5 "$2" >/dev/null 2>&1; then
    echo "[OK] $1"
  else
    echo "[FAIL] $1 探测失败 ($2)"
    FAILED=1
  fi
}

echo "===== exam-flow 考前巡检 $(date '+%Y-%m-%d %H:%M:%S') ====="

# 1. 基础设施连通性
check_port "MySQL"      "${DB_HOST:-127.0.0.1}" "${DB_PORT:-3306}"
check_port "Redis"      "${REDIS_HOST:-127.0.0.1}" "${REDIS_PORT:-6379}"
check_port "RocketMQ"   "${MQ_HOST:-127.0.0.1}" "${MQ_PORT:-9876}"
check_port "Nacos"      "${NACOS_HOST:-127.0.0.1}" "${NACOS_PORT:-8848}"
check_port "MinIO"      "${MINIO_HOST:-127.0.0.1}" "${MINIO_PORT:-9000}"

# 2. 核心服务健康(生产:网关地址)
check_http "网关健康检查" "${GATEWAY:-http://127.0.0.1:8080}/actuator/health"
check_http "考试服务"     "${EXAM_SERVICE:-http://127.0.0.1:8086}/actuator/health"

# TODO(接入业务系统后补充):
# - 试卷完整性校验(题量、分值合计 = 总分、快照可解压)
# - 场次配置校验(时间窗、容量、监考员在位)
# - 短信通道探测、人脸服务连通性、OSS 读写探测
# - 考试资源预加载(快照预热、Redis 内存预算)
# - 生成巡检报告并推送

if [ "${FAILED}" -eq 0 ]; then
  echo "===== 巡检通过,可以开考 ====="
  exit 0
else
  echo "===== 巡检未通过,禁止开考,请排查 ====="
  exit 1
fi
