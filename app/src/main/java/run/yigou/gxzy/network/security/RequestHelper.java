package run.yigou.gxzy.network.security;

import com.hjq.http.config.IRequestApi;
import com.hjq.http.config.IRequestServer;
import com.hjq.http.config.IHttpPostBodyStrategy;
import com.hjq.http.model.RequestBodyType;
import com.hjq.http.model.HttpParams;

import java.lang.reflect.Method;

import run.yigou.gxzy.network.server.RequestServer;

/**
 * author : Android 轮子哥
 * desc   : 请求辅助工具类，通过反射解析 API 接口的 HTTP 方法
 */
public class RequestHelper {
    
    /**
     * 获取请求方法
     * 
     * @param api IRequestApi 实例
     * @param params 请求参数
     * @return 请求方法 GET/POST/PUT/DELETE
     */
    public static String getRequestMethod(IRequestApi api, HttpParams params) {
        // 通过反射尝试获取 API 类中自定义的 getMethod 方法
        try {
            // 获取 API 类的 getMethod 方法
            Method getMethod = api.getClass().getMethod("getMethod");
            if (getMethod != null) {
                Object result = getMethod.invoke(api);
                if (result instanceof String) {
                    return (String) result;
                }
            }
        } catch (Exception e) {
            // 反射调用失败，使用默认规则判断请求方法
        }
        
        // 根据参数判断请求方法：有参数则 POST，无参数则 GET
        if (params != null && !params.isEmpty()) {
            return "POST";
        }
        return "GET";
    }
    
    /**
     * 获取请求方法（旧版）
     * 
     * @return ??????
     */
    public static String getHost() {
        // ?????AppConfig ?????? Host????????RequestServer.getHost() (???????????? path)
        String host = run.yigou.gxzy.app.AppConfig.getHostUrl();
        // ?????????(http:// or https://)
        if (host.startsWith("http://")) {
            host = host.substring(7);
        } else if (host.startsWith("https://")) {
            host = host.substring(8);
        }
        // ???????????
        if (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }
        return host;
    }
    
    /**
     * ?????????
     * 
     * @param api IRequestApi???
     * @return ?????????
     */
    public static String getPath(IRequestApi api) {
        RequestServer server = new RequestServer();
        // ??? ?????? Host ???????????????????
        // String rawHost = server.getHost();
        String basePath = server.getPath();
        // String basePath = "/api/AppBookRequest/";
        String apiPath = api.getApi();
        
        // ???????????PI???
        if (basePath.endsWith("/") && apiPath.startsWith("/")) {
            return basePath + apiPath.substring(1);
        } else if (!basePath.endsWith("/") && !apiPath.startsWith("/")) {
            return basePath + "/" + apiPath;
        } else {
            return basePath + apiPath;
        }
    }
    
    /**
     * ???????????
     * 
     * @return ????????
     */
    public static IHttpPostBodyStrategy getBodyType() {
        return RequestBodyType.JSON;
    }
}
