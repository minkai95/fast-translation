package com.plugin.fasttranslation.thirdparty;

import com.plugin.fasttranslation.setting.FastTranslationSettings;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.tmt.v20180321.TmtClient;
import com.tencentcloudapi.tmt.v20180321.models.LanguageDetectRequest;
import com.tencentcloudapi.tmt.v20180321.models.LanguageDetectResponse;
import com.tencentcloudapi.tmt.v20180321.models.TextTranslateRequest;
import com.tencentcloudapi.tmt.v20180321.models.TextTranslateResponse;
import kotlin.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestTencent {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestTencent.class);
    private static final String SECRET_ID = "";
    private static final String SECRET_KEY = "";
    private static final TmtClient client;

    static {
        // 实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
        // 代码泄露可能会导致 SecretId 和 SecretKey 泄露，并威胁账号下所有资源的安全性。以下代码示例仅供参考，建议采用更安全的方式来使用密钥，请参见：https://cloud.tencent.com/document/product/1278/85305
        // 密钥可前往官网控制台 https://console.cloud.tencent.com/cam/capi 进行获取
        Credential cred = new Credential(SECRET_ID, SECRET_KEY);
        // 实例化一个http选项，可选的，没有特殊需求可以跳过
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint("tmt.tencentcloudapi.com");
        // 实例化一个client选项，可选的，没有特殊需求可以跳过
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);
        // 实例化要请求产品的client对象,clientProfile是可选的
        client = new TmtClient(cred, "ap-beijing", clientProfile);
    }

    public static Pair<Boolean, String> translate(String text) {
        Pair<Boolean, String> pair = languageDetect(text);
        if (!pair.getFirst()) {
            return pair;
        }
        String sourceLang = pair.getSecond();
        if ("en".equalsIgnoreCase(sourceLang)) {
            return translate(text, "en", FastTranslationSettings.getInstance().nativeLanguage);
        }
        return translate(text, "auto", "en");
    }

    private static Pair<Boolean, String> translate(String text, String sourceLang, String targetLang) {
        try {
            TextTranslateRequest req = new TextTranslateRequest();
            req.setSourceText(text);
            req.setSource(sourceLang);
            req.setTarget(targetLang);
            req.setProjectId(0L);
            // 返回的resp是一个TextTranslateResponse的实例，与请求对象对应
            TextTranslateResponse resp = client.TextTranslate(req);
            return new Pair<>(true, resp.getTargetText());
        } catch (TencentCloudSDKException e) {
            LOGGER.warn("请求腾讯翻译失败,TextTranslate,text:{},sourceLang:{},targetLang:{}, msg:{}",
                    text, sourceLang, targetLang, e.toString());
            return new Pair<>(false, e.getMessage());
        }
    }

    private static Pair<Boolean, String> languageDetect(String text) {
        try {
            LanguageDetectRequest req = new LanguageDetectRequest();
            req.setText(text);
            req.setProjectId(0L);
            LanguageDetectResponse resp = client.LanguageDetect(req);
            return new Pair<>(true, resp.getLang());
        } catch (TencentCloudSDKException e) {
            LOGGER.warn("请求腾讯翻译失败,LanguageDetect,text:{}, msg:{}", text, e.toString());
            return new Pair<>(false, e.getMessage());
        }
    }

}
