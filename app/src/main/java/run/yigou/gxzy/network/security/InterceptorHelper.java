package run.yigou.gxzy.network.security;

import com.hjq.http.config.IRequestApi;
import com.hjq.http.model.RequestBodyType;
import com.hjq.http.model.HttpHeaders;
import com.hjq.http.model.HttpParams;

import java.util.Map;

import run.yigou.gxzy.network.server.RequestServer;
import run.yigou.gxzy.utils.SerialUtil;
import run.yigou.gxzy.base.constant.AppConst;
import run.yigou.gxzy.app.AppApplication;

/**
 * author : Android ?????
 * desc   : ??????????????????????????
 */
public class InterceptorHelper {

    /**
     * ????????????
     *
     * @param api            IRequestApi???
     * @param params         ??????
     * @param headers        ?????
     * @param appApplication ??????
     */
    public static void handleIntercept(IRequestApi api, HttpParams params, HttpHeaders headers, AppApplication appApplication) {

        //???????????????????????????????????????
        //if (appApplication.global_openness && appApplication.mUserInfoToken == null)
        //    headers.put("Authorization", AppConst.AllowAnonymous_Token);
        headers.put("app", "2");
        headers.put("SessionId", SerialUtil.getSerial());
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("Accept", "application/json, text/plain, */*");

        // ?????????????????
        if (SecurityConfig.isAntiReplayAttackEnabled()) {
            String accessKeyId = SecurityConfig.getAccessKeyId();
            String accessKeySecret = SecurityConfig.getAccessKeySecret();
            // ??? ?????????AccessKeyId??ccessKeySecret
            if (appApplication.mUserInfoToken != null){
                accessKeyId = appApplication.mUserInfoToken.getAccessKeyId();
                accessKeySecret = appApplication.mUserInfoToken.getAccessKeySecret();
                //??????????????????AccessKeyId??ccessKeySecret
                SecurityConfig.setAccessKeyId(accessKeyId);
                SecurityConfig.setAccessKeySecret(accessKeySecret);
            }
            // ?????????AccessKey????????????
            if (accessKeyId != null && !accessKeyId.isEmpty() &&
                    accessKeySecret != null && !accessKeySecret.isEmpty()) {

                // ????????????
                String method = RequestHelper.getRequestMethod(api, params);
                String host = RequestHelper.getHost();
                String path = RequestHelper.getPath(api);
                
                // ?????????Nonce (???2025-12????????????Method/Host/Path/Timestamp/Nonce)
                String timestamp = SecurityConfig.getCurrentTimestamp();
                String nonce = SecurityConfig.generateNonce();

                // ??????
                String signature = SecurityConfig.generateSignature(api, method, host, path, timestamp, nonce);

                // ??????????????
                headers.put("Signature", "Signature " + signature);
                headers.put("X-AccessKeyId", accessKeyId);
                headers.put("X-Timestamp", timestamp);
                headers.put("X-Nonce", nonce);
                
                // ????????M2???????????????
                if (SecurityConfig.isSM2Enabled()) {
                    headers.put("X-Encryption-Algorithm", "SM2");
                }
            }
        }
    }
}
