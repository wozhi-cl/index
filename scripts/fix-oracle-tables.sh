#!/bin/bash

# 定义颜色
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

# 函数：打印信息
log_info() {
    echo -e "${GREEN}[INFO] $1${NC}"
}

# 函数：打印错误
log_error() {
    echo -e "${RED}[ERROR] $1${NC}"
}

log_info "修复 Oracle 表问题..."

# 创建完整的 SQL 脚本
cat > /tmp/fix_oracle.sql << 'EOF'
-- 检查当前用户和权限
SELECT USER FROM DUAL;

-- 创建序列
CREATE SEQUENCE sample_data_seq START WITH 1 INCREMENT BY 1 NOCACHE;

-- 创建示例数据表
CREATE TABLE SAMPLE_DATA (
    ID NUMBER(19) PRIMARY KEY,
    NAME VARCHAR2(255) NOT NULL,
    DESCRIPTION VARCHAR2(500),
    STATUS VARCHAR2(50),
    CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建触发器
CREATE OR REPLACE TRIGGER SAMPLE_DATA_bir
BEFORE INSERT ON SAMPLE_DATA
FOR EACH ROW
BEGIN
  IF :NEW.ID IS NULL THEN
    SELECT sample_data_seq.NEXTVAL INTO :NEW.ID FROM DUAL;
  END IF;
END;
/

CREATE OR REPLACE TRIGGER SAMPLE_DATA_bur
BEFORE UPDATE ON SAMPLE_DATA
FOR EACH ROW
BEGIN
  :NEW.UPDATED_AT := CURRENT_TIMESTAMP;
END;
/

-- 插入示例数据
INSERT INTO SAMPLE_DATA (NAME, DESCRIPTION, STATUS) VALUES ('John Doe', 'Sample data 1', 'active');
INSERT INTO SAMPLE_DATA (NAME, DESCRIPTION, STATUS) VALUES ('Jane Smith', 'Sample data 2', 'inactive');
INSERT INTO SAMPLE_DATA (NAME, DESCRIPTION, STATUS) VALUES ('Bob Johnson', 'Sample data 3', 'active');
INSERT INTO SAMPLE_DATA (NAME, DESCRIPTION, STATUS) VALUES ('Alice Brown', 'Sample data 4', 'inactive');
INSERT INTO SAMPLE_DATA (NAME, DESCRIPTION, STATUS) VALUES ('Charlie Wilson', 'Sample data 5', 'active');

-- 提交事务
COMMIT;

-- 验证表和数据
SELECT 'SAMPLE_DATA table created' as status FROM DUAL;
SELECT COUNT(*) as record_count FROM SAMPLE_DATA;
SELECT * FROM SAMPLE_DATA;
EOF

# 复制到容器并执行
docker cp /tmp/fix_oracle.sql index-oracle:/tmp/fix_oracle.sql
docker exec index-oracle sqlplus system/oracle123@XEPDB1 @/tmp/fix_oracle.sql

# 验证结果
log_info "验证表和数据..."
docker exec index-oracle bash -c "echo 'SELECT COUNT(*) FROM SAMPLE_DATA;' | sqlplus -s system/oracle123@XEPDB1"

# 清理临时文件
rm -f /tmp/fix_oracle.sql

log_info "Oracle 表修复完成！"
