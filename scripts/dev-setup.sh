#!/bin/bash

# 开发环境设置脚本
set -e

echo "🚀 设置索引管理系统开发环境"

# 检查 Docker 和 Docker Compose
if ! command -v docker &> /dev/null; then
    echo "❌ Docker 未安装，请先安装 Docker"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose 未安装，请先安装 Docker Compose"
    exit 1
fi

# 创建必要的目录
echo "📁 创建必要的目录..."
mkdir -p logs data monitoring

# 构建应用
echo "🔨 构建应用..."
./mvnw clean package -DskipTests

# 启动服务
echo "🐳 启动 Docker Compose 服务..."
docker-compose up -d mysql elasticsearch redis

# 等待服务启动
echo "⏳ 等待服务启动..."
sleep 30

# 检查服务状态
echo "🔍 检查服务状态..."
docker-compose ps

# 显示访问信息
echo ""
echo "✅ 开发环境设置完成！"
echo ""
echo "📊 服务访问地址："
echo "  - 应用服务: http://localhost:8080"
echo "  - 应用健康检查: http://localhost:8080/actuator/health"
echo "  - 应用指标: http://localhost:8080/actuator/prometheus"
echo "  - Elasticsearch: http://localhost:9200"
echo "  - Kibana: http://localhost:5601"
echo "  - Prometheus: http://localhost:9090"
echo ""
echo "🔧 调试信息："
echo "  - 调试端口: 5005"
echo "  - 日志目录: ./logs"
echo "  - 数据目录: ./data"
echo ""
echo "📝 常用命令："
echo "  - 查看日志: docker-compose logs -f index-app"
echo "  - 重启应用: docker-compose restart index-app"
echo "  - 停止服务: docker-compose down"
echo "  - 清理数据: docker-compose down -v"
