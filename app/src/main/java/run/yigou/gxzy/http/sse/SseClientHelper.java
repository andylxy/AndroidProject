package run.yigou.gxzy.http.sse;

import androidx.annotation.NonNull;

import com.hjq.http.EasyConfig;
import run.yigou.gxzy.utils.EasyLog;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;

/**
 * SSE 客户端辅助类
 * 负责构建配置了 TLS 1.2 的 OkHttpClient
 */
public class SseClientHelper {

    private static final String TAG = "SseClientHelper";

    /**
     * 获取配置好的 OkHttpClient
     * 
     * ✅ 从 EasyConfig 获取基础 client，自动应用拦截器
     * ✅ 添加 SSE 特定配置（超时时间）
     * ✅ 为老版本 Android 添加 TLS 1.2 配置
     */
    @NonNull
    public static OkHttpClient createSseClient(String host) {
        EasyLog.print(TAG, "========== 构建 SSE OkHttpClient ==========");
        
        boolean isHttps = host.startsWith("https://");
        EasyLog.print(TAG, "当前环境: " + (isHttps ? "HTTPS" : "HTTP"));
        EasyLog.print(TAG, "目标地址: " + host);
        
        // ✅ 从 EasyConfig 获取基础 client（自动应用拦截器、签名等）
        OkHttpClient.Builder clientBuilder = EasyConfig.getInstance()
                .getClient()
                .newBuilder()
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(300, TimeUnit.SECONDS)
                .connectTimeout(30, TimeUnit.SECONDS);
        
        EasyLog.print(TAG, "已设置超时：读300秒，写300秒，连接30秒");
        
        // ✅ 为 HTTPS 配置 TLS 1.2（服务器只支持 TLS 1.2，不接受降级）
        if (isHttps) {
            try {
                // 只使用 TLS 1.2，不尝试降级（服务器有防降级保护）
                ConnectionSpec tls12Only = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                        .tlsVersions(TlsVersion.TLS_1_2)
                        .allEnabledCipherSuites()
                        .build();
                
                clientBuilder.connectionSpecs(Arrays.asList(tls12Only, ConnectionSpec.CLEARTEXT));
                
                // 为所有 Android 版本强制启用 TLS 1.2
                try {
                    SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
                    sslContext.init(null, null, null);
                    
                    // 获取默认的 TrustManager
                    javax.net.ssl.TrustManagerFactory tmf = javax.net.ssl.TrustManagerFactory.getInstance(
                            javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());
                    tmf.init((java.security.KeyStore) null);
                    X509TrustManager trustManager = (X509TrustManager) tmf.getTrustManagers()[0];
                    
                    clientBuilder.sslSocketFactory(new Tls12SocketFactory(sslContext.getSocketFactory()), trustManager);
                    EasyLog.print(TAG, "已配置 TLS 1.2 SSLSocketFactory");
                } catch (Exception e) {
                    EasyLog.print(TAG, "TLS 1.2 SSLSocketFactory 配置失败: " + e.getMessage());
                }
                
                EasyLog.print(TAG, "已配置 TLS 1.2 (仅)");
            } catch (Exception e) {
                EasyLog.print(TAG, "TLS配置异常: " + e.getMessage());
            }
        }
        
        return clientBuilder.build();
    }

    /**
     * TLS 1.2 Socket Factory - 为老版本 Android 强制启用 TLS 1.2
     */
    private static class Tls12SocketFactory extends SSLSocketFactory {
        private final SSLSocketFactory delegate;
        
        Tls12SocketFactory(SSLSocketFactory delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }
        
        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }
        
        @Override
        public java.net.Socket createSocket(java.net.Socket s, String host, int port, boolean autoClose) throws IOException {
            return enableTls12(delegate.createSocket(s, host, port, autoClose));
        }
        
        @Override
        public java.net.Socket createSocket(String host, int port) throws IOException {
            return enableTls12(delegate.createSocket(host, port));
        }
        
        @Override
        public java.net.Socket createSocket(String host, int port, java.net.InetAddress localHost, int localPort) throws IOException {
            return enableTls12(delegate.createSocket(host, port, localHost, localPort));
        }
        
        @Override
        public java.net.Socket createSocket(java.net.InetAddress host, int port) throws IOException {
            return enableTls12(delegate.createSocket(host, port));
        }
        
        @Override
        public java.net.Socket createSocket(java.net.InetAddress address, int port, java.net.InetAddress localAddress, int localPort) throws IOException {
            return enableTls12(delegate.createSocket(address, port, localAddress, localPort));
        }
        
        private java.net.Socket enableTls12(java.net.Socket socket) {
            if (socket instanceof SSLSocket) {
                // 只启用 TLS 1.2，服务器不接受降级
                ((SSLSocket) socket).setEnabledProtocols(new String[]{"TLSv1.2"});
            }
            return socket;
        }
    }
}
