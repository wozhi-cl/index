#!/bin/bash

# 蓝绿部署脚本
set -e

# 配置变量
NAMESPACE="index-system"
APP_NAME="index"
IMAGE_TAG="${1:-latest}"
DEPLOYMENT_NAME="index-app"
GREEN_DEPLOYMENT="index-app-green"
BLUE_DEPLOYMENT="index-app-blue"

echo "开始蓝绿部署 $APP_NAME (版本: $IMAGE_TAG)"

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

# 确定当前环境（蓝或绿）
CURRENT_COLOR=$(kubectl get deployment $DEPLOYMENT_NAME -n $NAMESPACE -o jsonpath='{.metadata.labels.color}' 2>/dev/null || echo "blue")
if [ "$CURRENT_COLOR" = "blue" ]; then
    NEW_COLOR="green"
    NEW_DEPLOYMENT=$GREEN_DEPLOYMENT
    OLD_DEPLOYMENT=$BLUE_DEPLOYMENT
else
    NEW_COLOR="blue"
    NEW_DEPLOYMENT=$BLUE_DEPLOYMENT
    OLD_DEPLOYMENT=$GREEN_DEPLOYMENT
fi

echo "当前环境: $CURRENT_COLOR"
echo "新环境: $NEW_COLOR"

# 创建新部署
echo "创建新部署 ($NEW_COLOR)..."
kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: $NEW_DEPLOYMENT
  namespace: $NAMESPACE
  labels:
    app: $APP_NAME
    color: $NEW_COLOR
    version: $IMAGE_TAG
spec:
  replicas: 3
  selector:
    matchLabels:
      app: $APP_NAME
      color: $NEW_COLOR
  template:
    metadata:
      labels:
        app: $APP_NAME
        color: $NEW_COLOR
        version: $IMAGE_TAG
    spec:
      containers:
      - name: $APP_NAME
        image: $APP_NAME:$IMAGE_TAG
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: JAVA_OPTS
          value: "-Xms1g -Xmx2g -XX:+UseG1GC"
        envFrom:
        - secretRef:
            name: index-secrets
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        volumeMounts:
        - name: config-volume
          mountPath: /app/config
        - name: log-volume
          mountPath: /var/log/index
      volumes:
      - name: config-volume
        configMap:
          name: index-config
      - name: log-volume
        emptyDir: {}
EOF

# 等待新部署就绪
echo "等待新部署就绪..."
kubectl rollout status deployment/$NEW_DEPLOYMENT -n $NAMESPACE --timeout=300s

# 健康检查
echo "执行健康检查..."
NEW_PODS=$(kubectl get pods -n $NAMESPACE -l app=$APP_NAME,color=$NEW_COLOR -o jsonpath='{.items[*].metadata.name}')
for pod in $NEW_PODS; do
    echo "检查 Pod: $pod"
    kubectl exec -n $NAMESPACE $pod -- curl -f http://localhost:8080/actuator/health || {
        echo "健康检查失败: $pod"
        exit 1
    }
done

# 切换流量
echo "切换流量到新环境..."
kubectl patch service index-service -n $NAMESPACE -p '{"spec":{"selector":{"color":"'$NEW_COLOR'"}}}'

# 等待流量切换完成
echo "等待流量切换完成..."
sleep 30

# 验证新环境
echo "验证新环境..."
kubectl get pods -n $NAMESPACE -l app=$APP_NAME,color=$NEW_COLOR
kubectl get service index-service -n $NAMESPACE

# 清理旧部署
echo "清理旧部署..."
kubectl delete deployment $OLD_DEPLOYMENT -n $NAMESPACE --ignore-not-found=true

# 更新主部署
echo "更新主部署..."
kubectl patch deployment $DEPLOYMENT_NAME -n $NAMESPACE -p '{"spec":{"template":{"metadata":{"labels":{"color":"'$NEW_COLOR'","version":"'$IMAGE_TAG'"}}}}}'

echo "蓝绿部署完成！"
echo "当前环境: $NEW_COLOR"
echo "版本: $IMAGE_TAG"
