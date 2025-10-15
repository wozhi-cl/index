package com.company.index.common.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 哈希工具类
 * 提供稳定哈希计算
 */
public class HashingUtil {

    /**
     * 计算哈希值
     */
    public static int calculateHash(String input, String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] hashBytes = md.digest(input.getBytes());
            
            // 转换为整数
            int hash = 0;
            for (byte b : hashBytes) {
                hash = hash * 31 + (b & 0xFF);
            }
            
            return Math.abs(hash);
        } catch (NoSuchAlgorithmException e) {
            // 如果算法不支持，使用简单的哈希
            return input.hashCode();
        }
    }

    /**
     * 计算 MD5 哈希
     */
    public static int calculateMD5Hash(String input) {
        return calculateHash(input, "MD5");
    }

    /**
     * 计算 SHA-1 哈希
     */
    public static int calculateSHA1Hash(String input) {
        return calculateHash(input, "SHA-1");
    }

    /**
     * 计算 SHA-256 哈希
     */
    public static int calculateSHA256Hash(String input) {
        return calculateHash(input, "SHA-256");
    }

    /**
     * 简单哈希（用于测试）
     */
    public static int calculateSimpleHash(String input) {
        int hash = 0;
        for (char c : input.toCharArray()) {
            hash = hash * 31 + c;
        }
        return Math.abs(hash);
    }
}
