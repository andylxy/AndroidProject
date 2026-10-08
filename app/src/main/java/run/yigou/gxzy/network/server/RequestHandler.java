package run.yigou.gxzy.network.server;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;

import com.google.gson.JsonSyntaxException;
import run.yigou.gxzy.R;
import run.yigou.gxzy.data.remote.model.HttpData;
import run.yigou.gxzy.manager.lifecycle.ActivityManager;
import run.yigou.gxzy.manager.device.DeviceNoticeManager;
import run.yigou.gxzy.manager.update.UpdateManager;
import run.yigou.gxzy.network.exception.DeviceRevokedException;
import run.yigou.gxzy.network.exception.HandledHttpFailure;
import run.yigou.gxzy.network.exception.VersionGateException;
import run.yigou.gxzy.ui.account.LoginActivity;
import com.hjq.gson.factory.GsonFactory;
import run.yigou.gxzy.log.EasyLog;

import com.hjq.http.request.HttpRequest;
import com.hjq.http.exception.CancelException;
import com.hjq.http.config.IRequestHandler;
import com.hjq.http.exception.DataException;
import com.hjq.http.exception.HttpException;
import com.hjq.http.exception.NetworkException;
import com.hjq.http.exception.ResponseException;
import com.hjq.http.exception.ResultException;
import com.hjq.http.exception.ServerException;
import com.hjq.http.exception.TimeoutException;
import com.hjq.http.exception.TokenException;
import com.tencent.mmkv.MMKV;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import okhttp3.Headers;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 *    author : Android 轮子哥
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2019/12/07
 *    desc   : 请求处理类
 */
public final class RequestHandler implements IRequestHandler {

    private final Application mApplication;
    private final MMKV mMmkv;

    // 响应语义（哪些失败已有专用提示）已移到 network.exception.HandledHttpFailure：
    // 那里是纯 JVM 可测的判定入口，也避免本类与异常类互相依赖。

    public RequestHandler(Application application) {
        mApplication = application;
        mMmkv = MMKV.mmkvWithID("http_cache_id");
    }

    @Override
    public Object requestSuccess(HttpRequest<?> request, Response response, Type type) throws Exception {

        if (Response.class.equals(type)) {
            return response;
        }

        if (!response.isSuccessful()) {
            // 版本门与设备吊销需要 App 侧联动处理，必须在抛异常前拦下（spec §7）：
            //   - 426：拉 /api/app/version 弹强制升级（UpdateManager 内部去重）；
            //   - 401 + X-Device-Revoked: 1：提示设备被禁用，**不**跳登录，避免死循环（ADR-0003）。
            // 这两类已由 UpdateManager 给出专用提示，故抛 HandledHttpException 子类：调用方
            // （AppActivity / AppFragment#onHttpFail）据此**不弹通用错误 toast**（票据 21）。
            // 抛的不是 TokenException，requestFail 不会跳登录页。
            final String httpError = mApplication.getString(R.string.http_response_error) + "，responseCode："
                    + response.code() + "，message：" + response.message();
            if (HandledHttpFailure.isHandled(response)) {
                if (response.code() == HandledHttpFailure.HTTP_UPGRADE_REQUIRED) {
                    UpdateManager.onVersionTooLow();
                    throw new VersionGateException(httpError);
                }
                DeviceNoticeManager.onDeviceRevoked();
                throw new DeviceRevokedException(httpError);
            }
            // 返回响应异常
            throw new ResponseException(httpError, response);
        }

        if (Headers.class.equals(type)) {
            return response.headers();
        }

        ResponseBody body = response.body();
        if (body == null) {
            return null;
        }

        if (InputStream.class.equals(type)) {
            return body.byteStream();
        }

        String text;
        try {
            text = body.string();
        } catch (IOException e) {
            // 返回结果读取异常
            throw new DataException(mApplication.getString(R.string.http_data_explain_error), e);
        }

        // 打印这个 Json 或者文本
        EasyLog.json(text);

        if (String.class.equals(type)) {
            return text;
        }

        if (JSONObject.class.equals(type)) {
            try {
                // 如果这是一个 JSONObject 对象
                return new JSONObject(text);
            } catch (JSONException e) {
                throw new DataException(mApplication.getString(R.string.http_data_explain_error), e);
            }
        }

        if (JSONArray.class.equals(type)) {
            try {
                // 如果这是一个 JSONArray 对象
                return new JSONArray(text);
            } catch (JSONException e) {
                throw new DataException(mApplication.getString(R.string.http_data_explain_error), e);
            }
        }

        final Object result;

        try {
            result = GsonFactory.getSingletonGson().fromJson(text, type);
        } catch (JsonSyntaxException e) {
            // 返回结果读取异常，将原始响应文本加入异常信息中以便调试
            throw new DataException(mApplication.getString(R.string.http_data_explain_error) + ", response: " + text, e);
        }

        if (result instanceof HttpData) {
            HttpData<?> model = (HttpData<?>) result;

            if (model.isRequestSucceed()) {
                // 代表执行成功
                return result;
            }

            if (model.isTokenFailure()) {
                // 代表登录失效，需要重新登录
                throw new TokenException(mApplication.getString(R.string.http_token_error));
            }

            // 代表执行失败
            throw new ResultException(model.getMessage(), model);
        }
        return result;
    }

    @Override
    public Throwable requestFail(HttpRequest<?> request, Throwable e) {
        // 判断这个异常是不是自己抛的
        if (e instanceof HttpException) {
            if (e instanceof TokenException) {
                // 登录信息失效，跳转到登录页
                Application application = ActivityManager.getInstance().getApplication();
                Intent intent = new Intent(application, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                application.startActivity(intent);
                // 销毁除了登录页之外的 Activity
                ActivityManager.getInstance().finishAllActivities(LoginActivity.class);
            }
            return e;
        }

        if (e instanceof SocketTimeoutException) {
            return new TimeoutException(mApplication.getString(R.string.http_server_out_time), e);
        }

        // OkHttp 的 callTimeout / readTimeout 抛的是 InterruptedIOException("timeout")，
        // **不是** SocketTimeoutException，所以上面那个分支接不到；它此前落进 IOException
        // 兜底被当成「取消」而静默，用户看不到任何反馈。实测记录见
        // .scratch/app-device-version/okhttp-cancel-probe.txt（票据 22 遗留项）。
        if (e instanceof InterruptedIOException) {
            return new TimeoutException(mApplication.getString(R.string.http_server_out_time), e);
        }

        if (e instanceof UnknownHostException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network activeNetwork = ((ConnectivityManager) mApplication.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetwork();
                if (activeNetwork == null || ((ConnectivityManager) mApplication.getSystemService(Context.CONNECTIVITY_SERVICE)).getNetworkCapabilities(activeNetwork) == null) {
                    // 没有连接就是网络异常
                    return new NetworkException(mApplication.getString(R.string.http_network_error), e);
                }
            } else {
                // For older versions, use deprecated method but it still works
                NetworkInfo info = ((ConnectivityManager) mApplication.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
                // 判断网络是否连接
                if (info == null || !info.isConnected()) {
                    // 没有连接就是网络异常
                    return new NetworkException(mApplication.getString(R.string.http_network_error), e);
                }
            }

            // 有连接就是服务器的问题
            return new ServerException(mApplication.getString(R.string.http_server_error), e);
        }

        // 后端不可达 / 连接被拒是**真实网络失败**，不能落进下面 IOException 的
        // CancelException 静默分支——EasyHttp 对 CancelException 跳过失败回调，
        // 用户点了书会毫无反馈（票据 22）。
        if (e instanceof ConnectException) {
            return new NetworkException(mApplication.getString(R.string.http_network_error), e);
        }

        if (e instanceof IOException) {
            //e = new CancelException(context.getString(R.string.http_request_cancel), e);
            // 其余 IOException 保持静默：OkHttp 主动取消请求常以普通 IOException 抛出，
            // 若一并转成网络错误，会把正常的取消误报成网络故障。
            return new CancelException("", e);
        }

        return new HttpException(e.getMessage(), e);
    }

    /*
    @Override
    public Object readCache(HttpRequest<?> request, Type type) {
        return null;
    }

    @Override
    public boolean writeCache(HttpRequest<?> request, Response response, Object result) {
        return false;
    }
    */
}