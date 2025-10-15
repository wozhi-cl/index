#!/bin/bash

# 运行所有组合测试的脚本

set -e

echo "================================================================"
echo "🧪 运行所有数据源和索引目标组合测试"
echo "================================================================"

# 切换到项目根目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_DIR="$( cd "$SCRIPT_DIR/.." && pwd )"
cd "$PROJECT_DIR"

# 测试组合列表
COMBINATIONS=(
    "H2→File(全量):FullIndexJobTest"
    "H2→File(增量):IncrementalIndexJobTest"
    "CSV→File:CsvToFileJobTest"
    "JSON→File:JsonToFileJobTest"
)

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 统计变量
TOTAL=0
PASSED=0
FAILED=0
SKIPPED=0

# 如果提供了参数，只运行特定的测试
if [ $# -gt 0 ]; then
    FILTER="$1"
    echo "📋 过滤条件: $FILTER"
    echo ""
fi

# 运行每个测试组合
for combo in "${COMBINATIONS[@]}"; do
    IFS=':' read -r name testClass <<< "$combo"
    
    # 如果有过滤条件，检查是否匹配
    if [ -n "$FILTER" ] && [[ ! "$name" =~ "$FILTER" ]] && [[ ! "$testClass" =~ "$FILTER" ]]; then
        continue
    fi
    
    TOTAL=$((TOTAL + 1))
    
    echo ""
    echo "============================================================"
    echo "🧪 测试组合 #$TOTAL: $name"
    echo "📝 测试类: $testClass"
    echo "============================================================"
    
    # 运行测试
    if mvn test -Dtest="$testClass" -q; then
        echo -e "${GREEN}✅ 测试通过: $name${NC}"
        PASSED=$((PASSED + 1))
    else
        EXIT_CODE=$?
        if [ $EXIT_CODE -eq 0 ]; then
            echo -e "${YELLOW}⏭️  测试跳过: $name${NC}"
            SKIPPED=$((SKIPPED + 1))
        else
            echo -e "${RED}❌ 测试失败: $name${NC}"
            FAILED=$((FAILED + 1))
        fi
    fi
done

# 打印测试总结
echo ""
echo "================================================================"
echo "📊 测试总结"
echo "================================================================"
echo "总计: $TOTAL"
echo -e "${GREEN}通过: $PASSED${NC}"
echo -e "${RED}失败: $FAILED${NC}"
echo -e "${YELLOW}跳过: $SKIPPED${NC}"

# 计算成功率
if [ $TOTAL -gt 0 ]; then
    SUCCESS_RATE=$(( (PASSED * 100) / TOTAL ))
    echo "成功率: ${SUCCESS_RATE}%"
fi

echo "================================================================"

# 根据结果返回退出码
if [ $FAILED -gt 0 ]; then
    echo -e "${RED}❌ 有测试失败！${NC}"
    exit 1
else
    echo -e "${GREEN}✅ 所有测试通过！${NC}"
    exit 0
fi

