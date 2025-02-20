package run.yigou.gxzy.ui.tips.aimsg;

import com.hjq.http.EasyConfig;

import okhttp3.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.*;

/**
 * OkHttpUtil是一个工具类，用于创建一个忽略SSL/TLS证书验证的OkHttpClient。
 */
public class OkHttpUtil {

    /**
     *     创建一个OkHttpClient实例，该实例忽略SSL/TLS证书验证。
     */
    public static final OkHttpClient okHttpClient = getUnsafeOkHttpClient();

    /**
     * 创建一个忽略SSL/TLS证书验证的OkHttpClient。
     *
     * @return 一个具有自定义不安全设置的OkHttpClient
     */
    private static OkHttpClient getUnsafeOkHttpClient() {
        OkHttpClient.Builder builder = EasyConfig.getInstance().getClient().newBuilder();
        builder.readTimeout(10000, TimeUnit.MILLISECONDS);
        builder.writeTimeout(10000, TimeUnit.MILLISECONDS);
        builder.connectTimeout(10000, TimeUnit.MILLISECONDS);
        return builder.build();
    }
}

