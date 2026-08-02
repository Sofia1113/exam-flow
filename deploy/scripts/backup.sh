#!/usr/bin/env bash
# ============================================================
# exam-flow 数据备份脚本(骨架,TDD §9 备份策略)
# 每日全量 + binlog 增量(RPO ≤ 5 分钟);生产环境建议接入
# 云备份/异地冷备,并每季度执行恢复演练。
# 用法:backup.sh [full|incremental]
# ============================================================
set -euo pipefail

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-root}"
DB_NAME="${DB_NAME:-exam_flow}"
BACKUP_DIR="${BACKUP_DIR:-/data/backup/exam_flow}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"

mkdir -p "${BACKUP_DIR}"
STAMP="$(date +%Y%m%d_%H%M%S)"

full_backup() {
  echo "[backup] 全量备份开始 ${STAMP}"
  mysqldump -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASS}" \
    --single-transaction --routines --triggers \
    "${DB_NAME}" | gzip > "${BACKUP_DIR}/full_${STAMP}.sql.gz"
  # 记录 binlog 位点,用于增量衔接
  mysql -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASS}" \
    -e "SHOW MASTER STATUS\G" > "${BACKUP_DIR}/binlog_pos_${STAMP}.txt"
  echo "[backup] 全量备份完成 $(du -h "${BACKUP_DIR}/full_${STAMP}.sql.gz" | cut -f1)"
}

incremental_backup() {
  echo "[backup] 增量备份(生产由 binlog 同步完成,本步骤为占位)"
  # 生产:开启 binlog 并配置从库/异地同步(mysqlbinlog 增量拉取),
  # 或使用云 RDS 的自动备份能力。
}

cleanup() {
  find "${BACKUP_DIR}" -name 'full_*.sql.gz' -mtime +"${RETENTION_DAYS}" -delete
  echo "[backup] 清理 ${RETENTION_DAYS} 天前备份完成"
}

case "${1:-full}" in
  full) full_backup; cleanup ;;
  incremental) incremental_backup ;;
  *) echo "用法: $0 [full|incremental]"; exit 1 ;;
esac
