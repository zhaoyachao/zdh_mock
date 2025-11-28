package com.zyc.mock.util;

import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ShortUrlUtil {

    public static String generateShortLink(String longLink) {
        // 使用 Murmurhash算法，进行哈希，得到长链接Hash值
        long longLinkHash = Hashing.murmur3_32().hashString(longLink, StandardCharsets.UTF_8).padToLong();
        // 如果Hash冲突则加随机盐再次Hash
        return regenerateOnHashConflict(longLink, longLinkHash);
    }
    // 参数1 长连接  参数2 生成的Hash
    private static String regenerateOnHashConflict(String longLink, long longLinkHash) {
        long id = System.currentTimeMillis();
        long uniqueIdHash = Hashing.murmur3_32().hashLong(id).padToLong();
        // 相减主要是为了让哈希值更小
        String shortLink = Base62Encoder.encode(Math.abs(longLinkHash - uniqueIdHash));
        return shortLink;
    }

    /**
     * 生成字符串的MD5哈希值
     * @param input 输入字符串
     * @return MD5哈希值（32位小写十六进制字符串）
     */
    public static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    /**
     * 字节数组转换为十六进制字符串
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
