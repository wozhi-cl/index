#!/bin/bash

# Oracle 用户创建脚本

echo "正在创建 Oracle 用户..."

docker exec index-oracle bash -c "cat <<EOF | sqlplus -s system/oracle123@XE
SET ECHO ON
CREATE USER index_user IDENTIFIED BY index123;
GRANT CONNECT, RESOURCE, DBA TO index_user;
GRANT UNLIMITED TABLESPACE TO index_user;
EXIT;
EOF
"

echo ""
echo "验证用户..."
docker exec index-oracle bash -c "cat <<EOF | sqlplus -s index_user/index123@XE
SELECT 'Connection OK' FROM DUAL;
EXIT;
EOF
"

echo ""
echo "✅ Oracle 用户设置完成！"

