#!/bin/bash

# Git忽略规则验证脚本

echo "======================================"
echo "Git忽略规则验证"
echo "======================================"
echo ""

# 检查.gitignore文件是否存在
if [ ! -f .gitignore ]; then
    echo "❌ .gitignore 文件不存在！"
    exit 1
fi

echo "✅ .gitignore 文件存在"
echo ""

# 检查Git仓库
echo "1. 检查Git仓库状态..."
if [ ! -d .git ]; then
    echo "⚠️  Git仓库未初始化"
    echo "   运行: git init"
else
    echo "✅ Git仓库已初始化"
fi
echo ""

# 验证关键文件是否被忽略
echo "2. 验证关键文件/目录是否被正确忽略..."

check_ignored() {
    local path=$1
    local description=$2
    
    if [ -e "$path" ]; then
        if git check-ignore -q "$path" 2>/dev/null; then
            echo "   ✅ $description: $path"
            return 0
        else
            echo "   ⚠️  $description 未被忽略: $path"
            return 1
        fi
    else
        echo "   ℹ️  $description 不存在: $path"
        return 2
    fi
}

# 检查各类文件
check_ignored "target/" "Maven构建目录"
check_ignored "logs/" "日志目录"
check_ignored "logs/application.log" "应用日志"
check_ignored ".DS_Store" "macOS系统文件"
check_ignored ".idea/" "IDEA配置"
check_ignored "*.iml" "IDEA模块文件"
check_ignored "output/" "输出目录"
check_ignored "data/*.db" "数据库文件"

echo ""

# 显示当前被忽略的文件数量
echo "3. 统计被忽略的文件..."
if [ -d .git ]; then
    IGNORED_COUNT=$(git status --ignored --porcelain 2>/dev/null | grep '^!!' | wc -l | tr -d ' ')
    if [ "$IGNORED_COUNT" -gt 0 ]; then
        echo "   📊 共有 $IGNORED_COUNT 个文件/目录被忽略"
        echo ""
        echo "   前10个被忽略的文件:"
        git status --ignored --porcelain 2>/dev/null | grep '^!!' | head -10 | sed 's/!! /   - /'
    else
        echo "   ℹ️  当前没有被忽略的文件"
    fi
else
    echo "   ⚠️  请先初始化Git仓库"
fi
echo ""

# 显示未跟踪的文件（应该提交的）
echo "4. 未跟踪的文件（可能需要提交）..."
if [ -d .git ]; then
    UNTRACKED_COUNT=$(git status --porcelain 2>/dev/null | grep '^??' | wc -l | tr -d ' ')
    if [ "$UNTRACKED_COUNT" -gt 0 ]; then
        echo "   📝 共有 $UNTRACKED_COUNT 个未跟踪的文件"
        echo ""
        echo "   主要的未跟踪文件:"
        git status --porcelain 2>/dev/null | grep '^??' | head -15 | sed 's/?? /   - /'
    else
        echo "   ✅ 所有文件都已跟踪或被忽略"
    fi
else
    echo "   ⚠️  请先初始化Git仓库"
fi
echo ""

# 显示.gitignore文件的统计信息
echo "5. .gitignore 文件统计..."
TOTAL_LINES=$(wc -l < .gitignore | tr -d ' ')
COMMENT_LINES=$(grep -c '^#' .gitignore)
EMPTY_LINES=$(grep -c '^$' .gitignore)
RULE_LINES=$((TOTAL_LINES - COMMENT_LINES - EMPTY_LINES))

echo "   📊 总行数: $TOTAL_LINES"
echo "   📝 注释行: $COMMENT_LINES"
echo "   ⚡ 规则行: $RULE_LINES"
echo "   ⬜ 空行: $EMPTY_LINES"
echo ""

# 显示.gitignore中的主要分类
echo "6. .gitignore 主要分类..."
echo "   - Maven构建文件"
echo "   - IDE配置文件 (IntelliJ, Eclipse, VS Code)"
echo "   - 日志和数据库文件"
echo "   - 应用输出文件"
echo "   - 操作系统文件 (macOS, Windows, Linux)"
echo "   - 安全敏感文件"
echo "   - 临时和备份文件"
echo ""

# 建议
echo "======================================"
echo "建议"
echo "======================================"
echo ""

if [ ! -d .git ]; then
    echo "📝 Git仓库未初始化，建议执行："
    echo "   git init"
    echo "   git add ."
    echo "   git commit -m 'Initial commit'"
elif [ "$UNTRACKED_COUNT" -gt 0 ]; then
    echo "📝 发现未跟踪的文件，建议："
    echo "   1. 查看文件列表: git status"
    echo "   2. 添加文件: git add ."
    echo "   3. 提交: git commit -m '描述你的更改'"
else
    echo "✅ Git配置正常！"
    echo ""
    echo "📝 后续操作："
    echo "   - 添加远程仓库: git remote add origin <URL>"
    echo "   - 推送代码: git push -u origin master"
fi
echo ""

# 显示有用的命令
echo "======================================"
echo "常用命令"
echo "======================================"
echo ""
echo "查看被忽略的文件:"
echo "  git status --ignored"
echo ""
echo "检查特定文件是否被忽略:"
echo "  git check-ignore -v <文件路径>"
echo ""
echo "查看完整的.gitignore内容:"
echo "  cat .gitignore"
echo ""
echo "编辑.gitignore:"
echo "  nano .gitignore"
echo "  或 vim .gitignore"
echo ""

echo "======================================"
echo "验证完成"
echo "======================================"

