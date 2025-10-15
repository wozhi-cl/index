package com.company.index.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 权限服务
 * 实现最小权限控制
 */
@Service
public class PermissionService {

    @Value("${index.security.adminUsers:admin}")
    private String adminUsers;

    @Value("${index.security.readOnlyUsers:readonly}")
    private String readOnlyUsers;

    // 用户权限缓存
    private final Map<String, List<String>> userPermissions = new ConcurrentHashMap<>();

    /**
     * 检查用户权限
     */
    public boolean hasPermission(String userId, String resource, String action) {
        List<String> permissions = getUserPermissions(userId);
        
        // 管理员拥有所有权限
        if (isAdmin(userId)) {
            return true;
        }
        
        // 检查具体权限
        String requiredPermission = resource + ":" + action;
        return permissions.contains(requiredPermission) || permissions.contains("*:*");
    }

    /**
     * 检查任务触发权限
     */
    public boolean canTriggerJob(String userId, String jobType) {
        return hasPermission(userId, "job", "trigger") || 
               hasPermission(userId, "job:" + jobType.toLowerCase(), "trigger");
    }

    /**
     * 检查数据源访问权限
     */
    public boolean canAccessDataSource(String userId, String dataSourceType) {
        return hasPermission(userId, "datasource", "read") || 
               hasPermission(userId, "datasource:" + dataSourceType.toLowerCase(), "read");
    }

    /**
     * 检查索引目标访问权限
     */
    public boolean canAccessIndexTarget(String userId, String indexTargetType) {
        return hasPermission(userId, "index", "write") || 
               hasPermission(userId, "index:" + indexTargetType.toLowerCase(), "write");
    }

    /**
     * 检查配置修改权限
     */
    public boolean canModifyConfig(String userId, String configKey) {
        return hasPermission(userId, "config", "write") || 
               hasPermission(userId, "config:" + configKey.toLowerCase(), "write");
    }

    /**
     * 检查审计查看权限
     */
    public boolean canViewAudit(String userId) {
        return hasPermission(userId, "audit", "read");
    }

    /**
     * 获取用户权限列表
     */
    public List<String> getUserPermissions(String userId) {
        return userPermissions.computeIfAbsent(userId, this::loadUserPermissions);
    }

    /**
     * 加载用户权限
     */
    private List<String> loadUserPermissions(String userId) {
        if (isAdmin(userId)) {
            return Arrays.asList("*:*");
        } else if (isReadOnly(userId)) {
            return Arrays.asList(
                "job:view",
                "datasource:read",
                "index:read",
                "audit:read"
            );
        } else {
            // 默认权限
            return Arrays.asList(
                "job:trigger",
                "datasource:read",
                "index:write"
            );
        }
    }

    /**
     * 检查是否为管理员
     */
    public boolean isAdmin(String userId) {
        return Arrays.asList(adminUsers.split(",")).contains(userId);
    }

    /**
     * 检查是否为只读用户
     */
    public boolean isReadOnly(String userId) {
        return Arrays.asList(readOnlyUsers.split(",")).contains(userId);
    }

    /**
     * 添加用户权限
     */
    public void addUserPermission(String userId, String permission) {
        List<String> permissions = getUserPermissions(userId);
        if (!permissions.contains(permission)) {
            permissions.add(permission);
            userPermissions.put(userId, permissions);
        }
    }

    /**
     * 移除用户权限
     */
    public void removeUserPermission(String userId, String permission) {
        List<String> permissions = getUserPermissions(userId);
        permissions.remove(permission);
        userPermissions.put(userId, permissions);
    }

    /**
     * 重置用户权限
     */
    public void resetUserPermissions(String userId) {
        userPermissions.remove(userId);
    }

    /**
     * 获取所有权限
     */
    public List<String> getAllPermissions() {
        return Arrays.asList(
            "job:trigger",
            "job:view",
            "job:cancel",
            "datasource:read",
            "datasource:write",
            "index:read",
            "index:write",
            "index:delete",
            "config:read",
            "config:write",
            "audit:read",
            "audit:write"
        );
    }
}
