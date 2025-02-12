package run.yigou.gxzy.ui.activity;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Looper;

import androidx.appcompat.widget.AppCompatTextView;

import com.hjq.http.EasyHttp;
import com.hjq.http.EasyLog;
import com.hjq.http.listener.HttpCallback;
import com.hjq.widget.layout.WrapRecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.common.AppConst;
import run.yigou.gxzy.greendao.entity.About;
import run.yigou.gxzy.greendao.util.ConvertEntity;
import run.yigou.gxzy.http.api.AboutApi;
import run.yigou.gxzy.http.model.HttpData;
import run.yigou.gxzy.ui.adapter.TipsUnitFragmentAdapter;
import run.yigou.gxzy.ui.dividerItemdecoration.CustomDividerItemDecoration;
import run.yigou.gxzy.ui.tips.tipsutils.TipsNetHelper;
import run.yigou.gxzy.utils.ThreadUtil;

/**
 * author : Android 轮子哥
 * github : https://github.com/getActivity/AndroidProject
 * time   : 2018/10/18
 * desc   : 关于界面
 */
public final class YaoUintActivity extends AppActivity {

    @Override
    protected int getLayoutId() {
        return R.layout.yao_uint_activity;
    }

    private WrapRecyclerView mTipsYaoUnitRecyclerView;
    private TipsUnitFragmentAdapter mTipsYaoUnitAdapter;


    @SuppressLint("SetTextI18n")
    @Override
    protected void initView() {
        mTipsYaoUnitRecyclerView = findViewById(R.id.tips_yao_uint);
        mTipsYaoUnitRecyclerView.addItemDecoration(new CustomDividerItemDecoration(AppConst.CustomDivider_BookList_RecyclerView_Color, AppConst.CustomDivider_Height));


    }

    @Override
    protected void initData() {
        mTipsYaoUnitAdapter = new TipsUnitFragmentAdapter(getContext());
        mTipsYaoUnitRecyclerView.setAdapter(mTipsYaoUnitAdapter);
        mTipsYaoUnitAdapter.setData(loadData());

    }

    private List<String> loadData() {
        String[] dataStrings = {"汉制一两约为 15.625克", "汉制一两为 24铢", "汉制一铢为 0.65克", "汉制一升约为 200毫升", "汉制一合为 20毫升", "杏仁一枚约为 0.4克", "1石=四钧＝29760克", "1钧=三十斤＝7440克", "1斤=248克", "1斤=16两", "1斤=液体250毫升", "1两=15.625克", "1两=24铢", "1升=液体200毫升", "1合=20毫升", "1圭=0.5克", "1龠=10毫升", "1撮=2克", "1方寸匕=金石类2.74克", "1方寸匕=药末约2克", "1方寸匕=草木类药末约1克", "半方寸匕=一刀圭=一钱匕=1.5克", "一钱匕=1.5-1.8克", "一铢=100个黍米的重量", "一分=3.9-4.2克", "梧桐子大约为 黄豆大", "蜀椒一升=50克", "葶力子一升=60克", "吴茱萸一升=50克", "五味子一升=50克", "半夏一升=130克", "虻虫一升=16克", "附子大者1枚=20-30克", "附子中者1枚=15克", "强乌头1枚小者=3克", "强乌头1枚大者=5-6克", "杏仁大者10枚=4克", "栀子10枚平均15克", "瓜蒌大小平均1枚=46克", "枳实1枚约14.4克", "石膏鸡蛋大1枚约40克", "厚朴1尺约30克", "竹叶一握约12克", "1斛=10斗＝20000毫升", "1斗=10升＝2000毫升", "1升=10合＝200毫升", "1合=2龠＝20毫升", "1龠=5撮＝10毫升", "1撮=4圭＝2毫升", "1圭=0.5毫升", "1引=10丈＝2310厘米", "1丈=10尺＝231厘米", "1尺=10寸＝23.1厘米", "1寸=10分＝2.31厘米", "1分=0.231厘米"};
        return new ArrayList<>(Arrays.asList(dataStrings));
    }

}