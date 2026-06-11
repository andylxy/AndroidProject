package run.yigou.gxzy.data.remote.model;

import java.util.List;

/**
 *    author : Android ?????
 *    github : https://github.com/getActivity/EasyHttp
 *    time   : 2020/10/07
 *    desc   : ???????????????
 */
public class HttpListData<T> extends HttpData<HttpListData.ListBean<T>> {

    public static class ListBean<T> {

        /** ?????? */
        private int pageIndex;
        /** ?????*/
        private int pageSize;
        /** ?????*/
        private int totalNumber;
        /** ??? */
        private List<T> items;

        /**
         * ??????????????
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
