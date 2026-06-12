package run.yigou.gxzy.data.remote.model;

import java.util.List;

/**
 *    author : Android 轮子哥
 *    github : https://github.com/getActivity/EasyHttp
 *    time   : 2020/10/07
 *    desc   : 分页列表响应数据模型
 */
public class HttpListData<T> extends HttpData<HttpListData.ListBean<T>> {

    public static class ListBean<T> {

        /** 当前页码 */
        private int pageIndex;
        /** 每页条数 */
        private int pageSize;
        /** 总条数 */
        private int totalNumber;
        /** 数据列表 */
        private List<T> items;

        /**
         * 判断是否为最后一页
         */
        public boolean isLastPage() {
            return Math.ceil((float) totalNumber / pageSize) <= pageIndex;
        }

        public int getTotalNumber() {
            return totalNumber;
        }

        public int getPageIndex() {
            return pageIndex;
        }

        public int getPageSize() {
            return pageSize;
        }

        public List<T> getItems() {
            return items;
        }
    }
}
