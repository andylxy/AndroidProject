package run.yigou.gxzy.sse;

import androidx.annotation.NonNull;

import run.yigou.gxzy.log.EasyLog;

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
 * 负责为 OkHttpClient 配置 TLS 1.2（为老版本 Android 强制启用）
 *
 * 注意：OkHttpClient 本身由调用方构建（来自 EasyConfig 或其他来源）
 * 此类只提供 TLS 配置增强功能
 */
public class SseClientHelper {

    private static final String TAG = "SseClientHelper";

    /**
     * 为 OkHttpClient 配置 TLS 1.2
     *
     * @param clientBuilder 原始的 OkHttpClient.Builder
     * @param host          目标服务器地址（判断是否 HTTPS）
     * @return 配置好的 OkHttpClient
     */
    @NonNull
    public static OkHttpClient configureTls12(@NonNull OkHttpClient.Builder clientBuilder, String host) {
        boolean isHttps = host.startsWith("https://");

        // 仅 HTTPS 需要配置 TLS
        if (!isHttps) {
            return clientBuilder.build();
        }

        try {
            // 只使用 TLS 1.2
            ConnectionSpec tls12Only = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .tlsVersions(TlsVersion.TLS_1_2)
                    .allEnabledCipherSuites()
                    .build();

            clientBuilder.connectionSpecs(Arrays.asList(tls12Only, ConnectionSpec.CLEARTEXT));

            // 为所有 Android 版本强制启用 TLS 1.2
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, null, null);

            // 获取默认的 TrustManager
            javax.net.ssl.TrustManagerFactory tmf = javax.net.ssl.TrustManagerFactory.getInstance(
                    javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((java.security.KeyStore) null);
            X509TrustManager trustManager = (X509TrustManager) tmf.getTrustManagers()[0];

            clientBuilder.sslSocketFactory(new Tls12SocketFactory(sslContext.getSocketFactory()), trustManager);
        } catch (Exception e) {
            EasyLog.print(TAG, "TLS 1.2 配置失败: " + e.getMessage());
        }

        return clientBuilder.build();
    }

    /**
     * TLS 1.2 Socket Factory
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
                ((SSLSocket) socket).setEnabledProtocols(new String[]{"TLSv1.2"});
            }
            return socket;
        }
    }
}
