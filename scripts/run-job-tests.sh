#!/bin/bash

# Job测试运行脚本

echo "======================================"
echo "Spring Batch Job 测试执行"
echo "======================================"
echo ""

# 设置颜色
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查Maven是否安装
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}❌ Maven未安装，请先安装Maven${NC}"
    exit 1
fi

echo -e "${YELLOW}📋 测试环境信息${NC}"
echo "Maven版本: $(mvn -v | head -1)"
echo "Java版本: $(java -version 2>&1 | head -1)"
echo ""

# 创建测试输出目录
echo "📁 创建测试输出目录..."
mkdir -p target/test-output
echo "✅ 输出目录: ./target/test-output"
echo ""

# 选项处理
TEST_CLASS=""
TEST_METHOD=""
VERBOSE=false
COVERAGE=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --full)
            TEST_CLASS="FullIndexJobTest"
            shift
            ;;
        --incremental)
            TEST_CLASS="IncrementalIndexJobTest"
            shift
            ;;
        --method)
            TEST_METHOD="$2"
            shift 2
            ;;
        --verbose)
            VERBOSE=true
            shift
            ;;
        --coverage)
            COVERAGE=true
            shift
            ;;
        --help)
            echo "使用方法:"
            echo "  $0 [选项]"
            echo ""
            echo "选项:"
            echo "  --full          仅运行全量索引Job测试"
            echo "  --incremental   仅运行增量索引Job测试"
            echo "  --method <名称> 运行指定的测试方法"
            echo "  --verbose       显示详细输出"
            echo "  --coverage      生成测试覆盖率报告"
            echo "  --help          显示此帮助信息"
            echo ""
            echo "示例:"
            echo "  $0                                    # 运行所有Job测试"
            echo "  $0 --full                             # 仅运行全量索引测试"
            echo "  $0 --incremental                      # 仅运行增量索引测试"
            echo "  $0 --full --method testFullIndexJobExecution  # 运行特定测试方法"
            echo "  $0 --coverage                         # 运行测试并生成覆盖率报告"
            exit 0
            ;;
        *)
            echo -e "${RED}未知选项: $1${NC}"
            echo "使用 --help 查看帮助"
            exit 1
            ;;
    esac
done

# 构建测试命令
if [ -n "$TEST_CLASS" ]; then
    if [ -n "$TEST_METHOD" ]; then
        TEST_PATTERN="${TEST_CLASS}#${TEST_METHOD}"
        echo -e "${YELLOW}🎯 运行测试: ${TEST_CLASS}.${TEST_METHOD}${NC}"
    else
        TEST_PATTERN="$TEST_CLASS"
        echo -e "${YELLOW}🎯 运行测试类: ${TEST_CLASS}${NC}"
    fi
else
    TEST_PATTERN="*JobTest"
    echo -e "${YELLOW}🎯 运行所有Job测试${NC}"
fi

echo ""

# 执行测试
START_TIME=$(date +%s)

if [ "$VERBOSE" = true ]; then
    MVN_ARGS=""
else
    MVN_ARGS="-q"
fi

echo -e "${YELLOW}▶️  开始执行测试...${NC}"
echo ""

if [ "$COVERAGE" = true ]; then
    echo -e "${YELLOW}📊 生成测试覆盖率报告...${NC}"
    mvn clean test jacoco:report -Dtest="$TEST_PATTERN" $MVN_ARGS
    TEST_EXIT_CODE=$?
    
    if [ $TEST_EXIT_CODE -eq 0 ]; then
        echo ""
        echo -e "${GREEN}📈 覆盖率报告已生成${NC}"
        echo "   报告位置: target/site/jacoco/index.html"
        
        # 尝试打开报告
        if command -v open &> /dev/null; then
            echo "   正在打开报告..."
            open target/site/jacoco/index.html
        fi
    fi
else
    mvn test -Dtest="$TEST_PATTERN" $MVN_ARGS
    TEST_EXIT_CODE=$?
fi

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo ""
echo "======================================"

# 检查测试结果
if [ $TEST_EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}✅ 所有测试通过！${NC}"
else
    echo -e "${RED}❌ 测试失败！${NC}"
    echo ""
    echo -e "${YELLOW}💡 提示:${NC}"
    echo "   1. 查看详细错误信息: mvn test -Dtest=$TEST_PATTERN"
    echo "   2. 查看测试日志: target/surefire-reports/"
    echo "   3. 使用 --verbose 参数查看完整输出"
fi

echo "⏱️  执行耗时: ${DURATION}秒"
echo "======================================"
echo ""

# 显示测试报告
if [ -d "target/surefire-reports" ]; then
    echo -e "${YELLOW}📊 测试报告统计${NC}"
    
    TOTAL_TESTS=$(find target/surefire-reports -name "TEST-*.xml" -exec grep -h "tests=" {} \; | \
                  sed 's/.*tests="\([0-9]*\)".*/\1/' | awk '{sum+=$1} END {print sum}')
    FAILED_TESTS=$(find target/surefire-reports -name "TEST-*.xml" -exec grep -h "failures=" {} \; | \
                   sed 's/.*failures="\([0-9]*\)".*/\1/' | awk '{sum+=$1} END {print sum}')
    ERROR_TESTS=$(find target/surefire-reports -name "TEST-*.xml" -exec grep -h "errors=" {} \; | \
                  sed 's/.*errors="\([0-9]*\)".*/\1/' | awk '{sum+=$1} END {print sum}')
    SKIPPED_TESTS=$(find target/surefire-reports -name "TEST-*.xml" -exec grep -h "skipped=" {} \; | \
                    sed 's/.*skipped="\([0-9]*\)".*/\1/' | awk '{sum+=$1} END {print sum}')
    
    if [ -n "$TOTAL_TESTS" ] && [ "$TOTAL_TESTS" -gt 0 ]; then
        PASSED_TESTS=$((TOTAL_TESTS - FAILED_TESTS - ERROR_TESTS - SKIPPED_TESTS))
        
        echo "   总测试数: $TOTAL_TESTS"
        echo -e "   ${GREEN}通过: $PASSED_TESTS${NC}"
        if [ "$FAILED_TESTS" -gt 0 ]; then
            echo -e "   ${RED}失败: $FAILED_TESTS${NC}"
        fi
        if [ "$ERROR_TESTS" -gt 0 ]; then
            echo -e "   ${RED}错误: $ERROR_TESTS${NC}"
        fi
        if [ "$SKIPPED_TESTS" -gt 0 ]; then
            echo -e "   ${YELLOW}跳过: $SKIPPED_TESTS${NC}"
        fi
    fi
    echo ""
    echo "   详细报告: target/surefire-reports/"
fi

# 显示测试输出文件
if [ -d "target/test-output" ]; then
    OUTPUT_COUNT=$(ls -1 target/test-output 2>/dev/null | wc -l | tr -d ' ')
    if [ "$OUTPUT_COUNT" -gt 0 ]; then
        echo ""
        echo -e "${YELLOW}📁 测试输出文件${NC}"
        echo "   位置: target/test-output/"
        ls -lh target/test-output/ | tail -n +2 | awk '{print "   - " $9 " (" $5 ")"}'
    fi
fi

echo ""

# 根据测试结果返回适当的退出码
exit $TEST_EXIT_CODE

