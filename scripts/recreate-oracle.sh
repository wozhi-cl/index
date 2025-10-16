#!/bin/bash

# 定义颜色
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

# 函数：打印信息
log_info() {
    echo -e "${GREEN}[INFO] $1${NC}"
}

# 函数：打印警告
log_warn() {
    echo -e "${YELLOW}[WARN] $1${NC}"
}

# 函数：打印错误
log_error() {
    echo -e "${RED}[ERROR] $1${NC}"
}

log_info "====================================="
log_info "重新创建 Oracle 容器"
log_info "====================================="

# 1. 停止并删除旧容器
log_info "步骤 1/4: 停止并删除旧的 Oracle 容器..."
docker compose stop oracle
docker compose rm -f oracle

# 2. 删除旧的数据卷（这会清除所有数据，重新初始化）
log_warn "步骤 2/4: 删除旧的数据卷（将清除所有数据）..."
docker volume rm index_oracle_data 2>/dev/null || log_info "数据卷不存在或已删除"

# 3. 重新启动容器
log_info "步骤 3/4: 启动新的 Oracle 容器..."
docker compose up -d oracle

# 4. 等待容器启动并健康
log_info "步骤 4/4: 等待 Oracle 容器启动（最多 120 秒）..."
log_warn "Oracle 首次启动可能需要 1-2 分钟，请耐心等待..."

for i in $(seq 1 24); do
    status=$(docker compose ps oracle | grep "Up (healthy)" | wc -l)
    if [ "$status" -gt 0 ]; then
        log_info "✅ Oracle 容器已启动并健康！"
        log_info "====================================="
        log_info "重新创建完成！"
        log_info "====================================="
        log_info "现在可以在 IDEA 中运行 Oracle 测试了。"
        exit 0
    fi
    echo -n "."
    sleep 5
done

log_error "Oracle 容器未能在规定时间内启动并健康！"
log_error "请检查容器日志：docker compose logs oracle"
exit 1

