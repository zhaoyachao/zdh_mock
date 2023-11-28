package com.zyc.mock.util;

import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;

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
}
