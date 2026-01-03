package run.yigou.gxzy.http.Server;

import run.yigou.gxzy.common.AppConst;
import run.yigou.gxzy.other.AppConfig;

import com.hjq.http.config.IRequestServer;
import run.yigou.gxzy.http.model.BodyType;

/**
 * author : Android 轮子哥
 * github : https://github.com/getActivity/AndroidProject
 * time   : 2020/10/02
 * desc   : 服务器配置
 */
public class RequestServer implements IRequestServer {

    @Override
    public String getHost() {
        return AppConfig.getHostUrl();
    }

    public String getPath() {
        return "/api/AppBookRequest/";
    }

    //    @Override
    //    public BodyType getType() {
    //        // 以表单的形式提交参数
    //        return BodyType.FORM;
    //    }
    // @Override
    public BodyType getType() {
        return BodyType.JSON;
    }
}