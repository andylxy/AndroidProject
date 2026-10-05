package run.yigou.gxzy.network.exception;

import com.hjq.http.exception.NetworkException;
import com.hjq.http.exception.ServerException;
import com.hjq.http.exception.TimeoutException;

import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * 「这次失败是不是**连不上服务器**」的唯一判定处。
 *
 * <p>为什么要区分：两种失败对用户的下一步动作完全不同 ——
 * 连不上 → 去查网络/后端地址（常见于装了指向另一个后端的包）；
 * 服务器出错 → 等一会再试。
 * 而原来两者共用一句「检查更新失败，请稍后重试」，用户与排查者都看不出是哪种，
 * 只能靠抓 logcat 才分得清。</p>
 *
 * <p>判定依据是 {@code RequestHandler.requestFail} 的既有映射（不要在这里重写一套网络知识）：
 * {@code ConnectException → NetworkException}、{@code SocketTimeout/InterruptedIO → TimeoutException}、
 * {@code UnknownHost + 有网络 → ServerException}。同时也看异常链上的 java.net 类型，
 * 这样即便将来库换了包装方式（EasyHttp 实测会把异常重包）判定依然成立。</p>
 *
 * <p><b>为什么单独一个类</b>：{@code UpdateManager} 有静态 Handler 初始化，纯 JVM 单测一旦
 * 引用它就会 {@code ExceptionInInitializerError}（票据 22 踩过）。放这里才能被单测覆盖。</p>
 */
public final class NetworkFailure {

    private NetworkFailure() {
    }

    /** 该失败是否属于「连不上服务器」（含超时、解析失败）。 */
    public static boolean isNetworkFailure(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof NetworkException
                    || cause instanceof TimeoutException
                    || cause instanceof ServerException
                    || cause instanceof UnknownHostException
                    || cause instanceof ConnectException
                    || cause instanceof SocketTimeoutException
                    || cause instanceof InterruptedIOException) {
                return true;
            }
        }
        return false;
    }
}
