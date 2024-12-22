package com.plugin.fasttranslation.util;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.MissingResourceException;

public class I18nUtil {
    private static final String BUNDLE_NAME = "messages";
    private static ResourceBundle bundle;
    private static Locale currentLocale;


    static {
        // 使用系统默认语言初始化
        setLocale(Locale.getDefault());
    }

    /**
     * 设置国际化的语言环境
     * @param locale 要设置的语言环境
     */
    public static void setLocale(Locale locale) {
        currentLocale = locale;
        bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale);
    }

    /**
     * 根据key获取当前语言环境下的消息
     * @param key 消息的键值
     * @return 本地化后的消息
     */
    public static String getMessage(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    /**
     * 根据key获取当前语言环境下的消息，支持参数替换
     * @param key 消息的键值
     * @param params 要替换的参数
     * @return 本地化并替换参数后的消息
     */
    public static String getMessage(String key, Object... params) {
        try {
            String message = bundle.getString(key);
            return String.format(message, params);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    /**
     * 获取当前的语言环境
     * @return 当前的语言环境
     */
    public static Locale getCurrentLocale() {
        return currentLocale;
    }

    /**
     * 检查指定的key是否存在对应的消息
     * @param key 要检查的消息键值
     * @return 如果消息存在返回true，否则返回false
     */
    public static boolean hasMessage(String key) {
        return bundle.containsKey(key);
    }
}
