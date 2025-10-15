#!/bin/bash

# 部署脚本
set -e

# 配置变量
NAMESPACE="index-system"
APP_NAME="index"
IMAGE_TAG="${1:-latest}"
DEPLOYMENT_NAME="index-app"

echo "开始部署 $APP_NAME (版本: $IMAGE_TAG)"

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

# 创建命名空间（如果不存在）
echo "创建命名空间..."
kubectl apply -f k8s/namespace.yaml

# 应用配置
echo "应用配置..."
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml

# 更新镜像标签
echo "更新部署镜像标签..."
kubectl set image deployment/$DEPLOYMENT_NAME -n $NAMESPACE $APP_NAME=$APP_NAME:$IMAGE_TAG

# 等待部署完成
echo "等待部署完成..."
kubectl rollout status deployment/$DEPLOYMENT_NAME -n $NAMESPACE --timeout=300s

# 应用服务
echo "应用服务..."
kubectl apply -f k8s/service.yaml

# 应用 CronJob
echo "应用 CronJob..."
kubectl apply -f k8s/cronjob.yaml

# 检查部署状态
echo "检查部署状态..."
kubectl get pods -n $NAMESPACE -l app=$APP_NAME
kubectl get services -n $NAMESPACE
kubectl get cronjobs -n $NAMESPACE

echo "部署完成！"
echo "应用访问地址:"
kubectl get service index-service-lb -n $NAMESPACE -o jsonpath='{.status.loadBalancer.ingress[0].ip}'
