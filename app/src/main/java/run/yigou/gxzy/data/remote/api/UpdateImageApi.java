package run.yigou.gxzy.data.remote.api;

import com.hjq.http.config.IRequestApi;

import java.io.File;

/**
 *    author : Android ?????
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2019/12/07
 *    desc   : ??????
 */
public final class UpdateImageApi implements IRequestApi {

    @Override
    public String getApi() {
        return "update/image";
    }
    // ????????????
    public String getMethod() {
        return "POST"; // ??"POST", "PUT" ??
    }
    /** ?????? */
    private File image;

    public UpdateImageApi setImage(File image) {
        this.image = image;
        return this;
    }
}
