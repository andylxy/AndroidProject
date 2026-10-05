package run.yigou.gxzy.network.exception;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.hjq.http.exception.DataException;
import com.hjq.http.exception.NetworkException;
import com.hjq.http.exception.ResponseException;
import com.hjq.http.exception.ServerException;
import com.hjq.http.exception.TimeoutException;

import org.junit.Test;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * 「连不上服务器」判定（纯 JVM）。
 *
 * <p>这层区分存在的理由：两种失败给用户的下一步完全不同 —— 连不上要去查网络/后端地址，
 * 服务器出错只要等一会。判错方向的代价是用户照着错的方向折腾。</p>
 */
public class NetworkFailureTest {

    @Test
    public void treatsConnectionAndDnsFailuresAsNetwork() {
        // RequestHandler.requestFail 的实际映射：连接被拒 → NetworkException
        assertTrue(NetworkFailure.isNetworkFailure(new NetworkException("连不上")));
        // 有网络但解析不了 → ServerException（文案是「服务器连接异常」，语义上仍是连不上）
        assertTrue(NetworkFailure.isNetworkFailure(new ServerException("解析失败")));
        assertTrue(NetworkFailure.isNetworkFailure(new ConnectException("refused")));
        assertTrue(NetworkFailure.isNetworkFailure(new UnknownHostException("no dns")));
    }

    @Test
    public void treatsTimeoutsAsNetwork() {
        assertTrue(NetworkFailure.isNetworkFailure(new TimeoutException("超时")));
        assertTrue(NetworkFailure.isNetworkFailure(new SocketTimeoutException("read timeout")));
        // OkHttp 的 callTimeout 抛 InterruptedIOException，**不是** SocketTimeoutException。
        assertTrue(NetworkFailure.isNetworkFailure(new InterruptedIOException("timeout")));
    }

    @Test
    public void looksThroughTheCauseChain() {
        // EasyHttp 会把异常重包：判定必须能穿透。
        Throwable wrapped = new RuntimeException("wrapper", new ConnectException("refused"));
        assertTrue(NetworkFailure.isNetworkFailure(wrapped));
    }

    @Test
    public void doesNotTreatServerErrorsAsNetwork() {
        // 404 / 500 说明**服务器是通的**，只是回答不对 —— 这时让用户去查网络是误导。
        assertFalse(NetworkFailure.isNetworkFailure(new ResponseException("404", null)));
        assertFalse(NetworkFailure.isNetworkFailure(new DataException("解析失败")));
        assertFalse(NetworkFailure.isNetworkFailure(new IOException("cancelled")));
        assertFalse(NetworkFailure.isNetworkFailure(new RuntimeException("boom")));
        assertFalse(NetworkFailure.isNetworkFailure(null));
    }
}
