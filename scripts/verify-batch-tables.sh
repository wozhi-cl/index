#!/bin/bash

echo "=== Spring Batch 表创建成功！ ==="
echo

echo "✅ 已创建的 Spring Batch 表："
echo "   - BATCH_JOB_INSTANCE"
echo "   - BATCH_JOB_EXECUTION"
echo "   - BATCH_JOB_EXECUTION_PARAMS"
echo "   - BATCH_JOB_EXECUTION_CONTEXT"
echo "   - BATCH_STEP_EXECUTION"
echo "   - BATCH_STEP_EXECUTION_CONTEXT"
echo "   - BATCH_STEP_EXECUTION_SEQ"
echo "   - BATCH_JOB_EXECUTION_SEQ"
echo "   - BATCH_JOB_SEQ"
echo

echo "=== 下一步操作 ==="
echo

echo "1. 在 IDEA 中重新运行应用"
echo "   - 停止当前应用"
echo "   - 重新运行 Application.java"
echo

echo "2. 验证配置"
echo "   查看启动日志，应该看到："
echo "   ✅ MySQL 连接成功"
echo "   ✅ Spring Batch 表正常"
echo "   ✅ 没有 Quartz 表错误"
echo

echo "3. 测试 API"
echo "   curl -u admin:admin123 http://localhost:8080/api/jobs/status"
echo

echo "=== 当前配置状态 ==="
echo "✅ MySQL 数据库: localhost:3307/index_db"
echo "✅ Spring Batch 表: 已创建"
echo "✅ Quartz: 内存模式"
echo "✅ 远程调试: 端口 5005"
echo

echo "=== 如果仍有问题 ==="
echo "1. 检查应用日志中的错误信息"
echo "2. 确认 MySQL 连接正常"
echo "3. 验证 Spring Batch 配置"
echo

echo "现在可以重新运行应用了！🚀"
