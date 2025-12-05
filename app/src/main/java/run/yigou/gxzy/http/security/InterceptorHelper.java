package run.yigou.gxzy.http.security;

import com.hjq.http.config.IRequestApi;
import com.hjq.http.model.BodyType;
import com.hjq.http.model.HttpHeaders;
import com.hjq.http.model.HttpParams;

import java.util.Map;

import run.yigou.gxzy.http.Server.RequestServer;
import run.yigou.gxzy.utils.SerialUtil;
import run.yigou.gxzy.common.AppConst;
import run.yigou.gxzy.app.AppApplication;

/**
 * author : Android 轮子哥
 * desc   : 拦截器辅助类，用于处理请求拦截逻辑
 */
public class InterceptorHelper {

    /**
     * 处理请求拦截逻辑
     *
     * @param api            IRequestApi对象
     * @param params         请求参数
     * @param headers        请求头
     * @param appApplication 应用实例
     */
    public static void handleIntercept(IRequestApi api, HttpParams params, HttpHeaders headers, AppApplication appApplication) {
        // 添加全局请求头
        if (appApplication.mUserInfoToken != null)
            headers.put("Authorization", appApplication.mUserInfoToken.getToken());
        //如果是全局开启，并且没有登陆就添加可获取全部的数据
        if (appApplication.global_openness && appApplication.mUserInfoToken == null)
            headers.put("Authorization", AppConst.AllowAnonymous_Token);
        headers.put("app", "2");
        headers.put("SessionId", SerialUtil.getSerial());
        // headers.put("versionName", AppConfig.getVersionName());
        // headers.put("versionCode", String.valueOf(AppConfig.getVersionCode()));
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("Accept", "application/json, text/plain, */*");

        // 添加防重放攻击相关头部
        if (SecurityConfig.isAntiReplayAttackEnabled()) {
            String accessKeyId = SecurityConfig.getAccessKeyId();
            String accessKeySecret = SecurityConfig.getAccessKeySecret();

            // 只有当配置了AccessKey时才添加相关头部
            if (accessKeyId != null && !accessKeyId.isEmpty() &&
                    accessKeySecret != null && !accessKeySecret.isEmpty()) {

                // 获取请求相关信息
                String method = RequestHelper.getRequestMethod(api, params);
                String host = RequestHelper.getHost();
                String path = RequestHelper.getPath(api);
                String queryString = SecurityConfig.buildQueryString(api, params);

                // 构建请求体
                BodyType bodyType = RequestHelper.getBodyType();
                String bodyString = SecurityConfig.buildBodyString(api, params, bodyType);

                // 生成签名
                String signature = SecurityConfig.generateSignature(api, method, host, path, queryString, bodyString);

                // 添加防重放攻击头部
                headers.put("Signature", "Signature " + signature);
                headers.put("X-AccessKeyId", accessKeyId);
                headers.put("X-Timestamp", SecurityConfig.getCurrentTimestamp());
                headers.put("X-Nonce", SecurityConfig.generateNonce());
            }
        }

        // 添加全局请求参数
        // params.put("6666666", "6666666");
    }
}