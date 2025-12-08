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

        //如果是全局开启，并且没有登陆就添加可获取全部的数据
        //if (appApplication.global_openness && appApplication.mUserInfoToken == null)
        //    headers.put("Authorization", AppConst.AllowAnonymous_Token);
        headers.put("app", "2");
        headers.put("SessionId", SerialUtil.getSerial());
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("Accept", "application/json, text/plain, */*");

        // 添加防重放攻击相关头部
        if (SecurityConfig.isAntiReplayAttackEnabled()) {
            String accessKeyId = SecurityConfig.getAccessKeyId();
            String accessKeySecret = SecurityConfig.getAccessKeySecret();
            // 如果 用登陆用户的AccessKeyId和AccessKeySecret
            if (appApplication.mUserInfoToken != null){
                accessKeyId = appApplication.mUserInfoToken.getAccessKeyId();
                accessKeySecret = appApplication.mUserInfoToken.getAccessKeySecret();
                //同一用户，则用登陆用户的AccessKeyId和AccessKeySecret
                SecurityConfig.setAccessKeyId(accessKeyId);
                SecurityConfig.setAccessKeySecret(accessKeySecret);
            }
            // 只有当配置了AccessKey时才添加相关头部
            if (accessKeyId != null && !accessKeyId.isEmpty() &&
                    accessKeySecret != null && !accessKeySecret.isEmpty()) {

                // 获取请求相关信息
                String method = RequestHelper.getRequestMethod(api, params);
                String host = RequestHelper.getHost();
                String path = RequestHelper.getPath(api);
                
                // 生成时间戳和Nonce (根据2025-12变更，签名仅包含Method/Host/Path/Timestamp/Nonce)
                String timestamp = SecurityConfig.getCurrentTimestamp();
                String nonce = SecurityConfig.generateNonce();

                // 生成签名
                String signature = SecurityConfig.generateSignature(api, method, host, path, timestamp, nonce);

                // 添加防重放攻击头部
                headers.put("Signature", "Signature " + signature);
                headers.put("X-AccessKeyId", accessKeyId);
                headers.put("X-Timestamp", timestamp);
                headers.put("X-Nonce", nonce);
                
                // 如果启用了SM2算法，则添加相应标识
                if (SecurityConfig.isSM2Enabled()) {
                    headers.put("X-Encryption-Algorithm", "SM2");
                }
            }
        }
    }
}