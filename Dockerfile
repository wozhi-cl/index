# 多阶段构建 Dockerfile
FROM openjdk:17-jdk-slim as builder

# 设置工作目录
WORKDIR /app

# 复制 Maven 配置文件
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# 下载依赖（利用 Docker 缓存）
RUN ./mvnw dependency:go-offline -B

# 复制源代码
COPY src ./src

# 构建应用
RUN ./mvnw clean package -DskipTests

# 运行时镜像
FROM openjdk:17-jre-slim

# 创建应用用户
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 设置工作目录
WORKDIR /app

# 复制构建的 JAR 文件
COPY --from=builder /app/target/*.jar app.jar

# 创建日志目录
RUN mkdir -p /var/log/index && chown -R appuser:appuser /var/log/index

# 创建数据目录
RUN mkdir -p /data && chown -R appuser:appuser /data

# 切换到应用用户
USER appuser

# 设置环境变量
ENV JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication"
ENV SPRING_PROFILES_ACTIVE=prod
ENV LOG_PATH=/var/log/index

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 暴露端口
EXPOSE 8080

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
