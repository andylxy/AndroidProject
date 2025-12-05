package run.yigou.gxzy.http.security;

import com.hjq.http.config.IRequestApi;
import com.hjq.http.config.IRequestServer;
import com.hjq.http.model.BodyType;
import com.hjq.http.model.HttpParams;

import java.util.Map;

import run.yigou.gxzy.http.Server.RequestServer;

/**
 * author : Android 轮子哥
 * desc   : 请求辅助类，用于提取请求相关信息
 */
public class RequestHelper {
    
    /**
     * 获取请求方法
     * 
     * @param api IRequestApi对象
     * @param params 请求参数
     * @return 请求方法（GET/POST）
     */
    public static String getRequestMethod(IRequestApi api, HttpParams params) {
        // 根据参数判断请求方法
        if (params != null && !params.isEmpty()) {
            return "POST";
        }
        return "GET";
    }
    
    /**
     * 获取主机地址
     * 
     * @return 主机地址
     */
    public static String getHost() {
        IRequestServer server = new RequestServer();
        String host = server.getHost();
        // 移除协议部分(http:// or https://)
        if (host.startsWith("http://")) {
            host = host.substring(7);
        } else if (host.startsWith("https://")) {
            host = host.substring(8);
        }
        // 移除末尾的斜杠
        if (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }
        return host;
    }
    
    /**
     * 获取请求路径
     * 
     * @param api IRequestApi对象
     * @return 完整请求路径
     */
    public static String getPath(IRequestApi api) {
        IRequestServer server = new RequestServer();
        String basePath = server.getPath();
        String apiPath = api.getApi();
        
        // 组合基础路径和API路径
        if (basePath.endsWith("/") && apiPath.startsWith("/")) {
            return basePath + apiPath.substring(1);
        } else if (!basePath.endsWith("/") && !apiPath.startsWith("/")) {
            return basePath + "/" + apiPath;
        } else {
            return basePath + apiPath;
        }
    }
    
    /**
     * 获取请求体类型
     * 
     * @return 请求体类型
     */
    public static BodyType getBodyType() {
        IRequestServer server = new RequestServer();
        return server.getType();
    }
}