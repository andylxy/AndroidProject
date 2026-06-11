package run.yigou.gxzy.data.remote.model;

/**
 *    author : Android ?????
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2019/12/07
 *    desc   : ????????????
 */
public class HttpData<T> {

    /** ?????*/
    private int code;
    /** ?????*/
    private String msg;
    /** ??? */
    private T data;

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return msg;
    }

    public T getData() {
        return data;
    }

    /**
     * ?????????
     */
    public boolean isRequestSucceed() {
        return code == 200;
    }

    /**
     * ??? Token ???
     */
    public boolean isTokenFailure() {
        return code == 1001;
    }
}
