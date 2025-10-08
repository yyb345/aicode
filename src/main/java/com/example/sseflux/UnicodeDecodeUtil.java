package com.example.sseflux;

import org.apache.commons.text.StringEscapeUtils;

public class UnicodeDecodeUtil {

    /**
     * 将包含 XXXX 的 Java Unicode 转义字符串解码为普通字符串
     * 例如：
     * 转为：如何提升锂电池的密度？
     *
     * @param input 含有 Unicode 转义的字符串
     * @return 解码后的正常字符串
     */
    public static String decodeUnicode(String input) {
        if (input == null) {
            return null;
        }
        return StringEscapeUtils.unescapeJava(input);
    }

    /** 反向：把中文等字符转为 XXXX 形式（以及转义控制字符、引号等） */
    public static String encodeToUnicodeEscapes(String input) {
        if (input == null) return null;
        return StringEscapeUtils.escapeJava(input);
    }

    // 示例
    public static void main(String[] args) {
        String raw = "\\u5982\\u4f55\\u63d0\\u5347\\u9502\\u7535\\u6c60\\u7684\\u5bc6\\u5ea6\\uff1f";
        String result = decodeUnicode(raw);
        System.out.println(result); // 输出：如何提升锂电池的密度？


        raw = " 如何开发一种新型电极材料或结构，能在显著提升锂电池能量密度的同时，保持或提高其安全性和循环寿命？";
        result = encodeToUnicodeEscapes(raw);
        System.out.println(result); // 输出：
    }
}
