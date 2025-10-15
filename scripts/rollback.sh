#!/bin/bash

# 回滚脚本
set -e

# 配置变量
NAMESPACE="index-system"
APP_NAME="index"
DEPLOYMENT_NAME="index-app"

echo "开始回滚 $APP_NAME"

# 检查 kubectl 是否可用
if ! command -v kubectl &> /dev/null; then
    echo "错误: kubectl 未安装或不在 PATH 中"
    exit 1
fi

# 检查集群连接
if ! kubectl cluster-info &> /dev/null; then
    echo "错误: 无法连接到 Kubernetes 集群"
    exit 1
fi

# 获取当前部署历史
echo "获取部署历史..."
kubectl rollout history deployment/$DEPLOYMENT_NAME -n $NAMESPACE

# 回滚到上一个版本
echo "回滚到上一个版本..."
kubectl rollout undo deployment/$DEPLOYMENT_NAME -n $NAMESPACE

# 等待回滚完成
echo "等待回滚完成..."
kubectl rollout status deployment/$DEPLOYMENT_NAME -n $NAMESPACE --timeout=300s

# 检查回滚状态
echo "检查回滚状态..."
kubectl get pods -n $NAMESPACE -l app=$APP_NAME
kubectl get deployment $DEPLOYMENT_NAME -n $NAMESPACE

echo "回滚完成！"

# 可选：回滚到特定版本
if [ ! -z "$1" ]; then
    echo "回滚到版本: $1"
    kubectl rollout undo deployment/$DEPLOYMENT_NAME -n $NAMESPACE --to-revision=$1
    kubectl rollout status deployment/$DEPLOYMENT_NAME -n $NAMESPACE --timeout=300s
    echo "回滚到版本 $1 完成！"
fi
