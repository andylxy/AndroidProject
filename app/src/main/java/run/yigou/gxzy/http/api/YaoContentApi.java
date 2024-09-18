

package run.yigou.gxzy.http.api;

import com.hjq.http.config.IRequestApi;

import run.yigou.gxzy.ui.tips.tipsutils.DataBeans.Yao;
import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;


public final class YaoContentApi implements IRequestApi {

    @Override
    public String getApi() {
        return "ZhongYao/GetAllZhongYao";
    }
    public final static class Bean extends Yao {

    }
}