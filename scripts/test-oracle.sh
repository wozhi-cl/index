#!/bin/bash

# Oracle 测试脚本
# 用于启动 Oracle 服务并运行测试

set -e

echo "========================================="
echo "Oracle 集成测试"
echo "========================================="

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 检查 Docker 是否运行
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}错误: Docker 未运行，请先启动 Docker${NC}"
    exit 1
fi

# 启动 Oracle 服务
echo -e "${YELLOW}步骤 1: 启动 Oracle 服务...${NC}"
docker-compose up -d oracle

# 等待 Oracle 启动
echo -e "${YELLOW}步骤 2: 等待 Oracle 启动 (可能需要 1-2 分钟)...${NC}"
echo "Oracle 首次启动可能需要较长时间，请耐心等待..."

MAX_RETRIES=60
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if docker exec index-oracle healthcheck.sh > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Oracle 已就绪！${NC}"
        break
    fi
    
    RETRY_COUNT=$((RETRY_COUNT + 1))
    echo -n "."
    sleep 5
    
    if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
        echo -e "${RED}错误: Oracle 启动超时${NC}"
        docker-compose logs oracle
        exit 1
    fi
done

echo ""

# 显示 Oracle 连接信息
echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}Oracle 连接信息:${NC}"
echo -e "  主机: localhost"
echo -e "  端口: 1521"
echo -e "  SID: XE"
echo -e "  用户名: index_user"
echo -e "  密码: index123"
echo -e "  JDBC URL: jdbc:oracle:thin:@localhost:1521:XE"
echo -e "${GREEN}=========================================${NC}"
echo ""

# 运行测试
echo -e "${YELLOW}步骤 3: 运行 Oracle 测试...${NC}"

# 选择要运行的测试
if [ "$1" == "file" ]; then
    echo "运行 Oracle → File 测试..."
    mvn test -Dtest=OracleToFileJobTest
elif [ "$1" == "es" ]; then
    echo "运行 Oracle → Elasticsearch 测试（需要先启动 Elasticsearch）..."
    docker-compose up -d elasticsearch
    sleep 10
    mvn test -Dtest=OracleToElasticsearchJobTest
elif [ "$1" == "all" ]; then
    echo "运行所有 Oracle 测试..."
    mvn test -Dtest=OracleToFileJobTest,OracleToElasticsearchJobTest
else
    echo "用法: $0 [file|es|all]"
    echo "  file - 运行 Oracle → File 测试"
    echo "  es   - 运行 Oracle → Elasticsearch 测试"
    echo "  all  - 运行所有 Oracle 测试"
    exit 0
fi

# 显示测试结果
if [ $? -eq 0 ]; then
    echo -e "${GREEN}=========================================${NC}"
    echo -e "${GREEN}✓ 测试通过！${NC}"
    echo -e "${GREEN}=========================================${NC}"
else
    echo -e "${RED}=========================================${NC}"
    echo -e "${RED}✗ 测试失败${NC}"
    echo -e "${RED}=========================================${NC}"
    exit 1
fi

# 询问是否停止服务
echo ""
read -p "是否停止 Oracle 服务? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "停止 Oracle 服务..."
    docker-compose stop oracle
    echo -e "${GREEN}✓ 服务已停止${NC}"
fi

