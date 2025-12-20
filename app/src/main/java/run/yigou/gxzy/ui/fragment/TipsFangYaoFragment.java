package run.yigou.gxzy.ui.fragment;

import android.annotation.SuppressLint;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.hjq.http.EasyLog;
import com.hjq.widget.layout.WrapRecyclerView;
import com.hjq.widget.view.ClearEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.app.TitleBarFragment;
import run.yigou.gxzy.common.AppConst;
import run.yigou.gxzy.ui.adapter.TipsFangYaoAdapter;
import run.yigou.gxzy.ui.dividerItemdecoration.CustomDividerItemDecoration;
import run.yigou.gxzy.ui.tips.data.BookData;
import run.yigou.gxzy.ui.tips.data.BookDataManager;
import run.yigou.gxzy.ui.tips.data.ChapterData;
import run.yigou.gxzy.ui.tips.data.GlobalDataHolder;
import run.yigou.gxzy.ui.tips.tipsutils.DataItem;
import run.yigou.gxzy.utils.DebugLog;


public final class TipsFangYaoFragment extends TitleBarFragment<AppActivity> {

    public static final String TAG = "TipsFangYaoFragment";
    /**
     * 1 是方 2 是药 , 3 是汉制单位
     */
    private int typeFangYao;
    private int bookId;
    private TextView numTips;
    private WrapRecyclerView mRecyclerView;
    private Button tipsBtnSearch;
    private ClearEditText clearEditText;
    private TextView ll_no_btn;
    // 使用 for 循环查找包含 SearchText控件指定内容
    List<String> searchTextResults = new ArrayList<>();

    /**
     * 创建一个实例，并设置类型。
     *
     * @param typeFangYao 1 是方 2 是药,3 是汉制单位
     * @return TipsFangYaoFragment 实例
     */
    public static TipsFangYaoFragment newInstance(int typeFangYao, int bookId) {
        TipsFangYaoFragment fragment = new TipsFangYaoFragment();
        fragment.typeFangYao = typeFangYao;
        fragment.bookId = bookId;
        return fragment;
    }


    @Override
    protected int getLayoutId() {
        return R.layout.tips_fragment_fang_yao;
    }

    @Override
    protected void initView() {
        ll_no_btn = findViewById(R.id.ll_no_btn);
        mRecyclerView = findViewById(R.id.tips_book_fang_yao_list);
        clearEditText = findViewById(R.id.searchEditText);
        tipsBtnSearch = findViewById(R.id.tips_btn_search);
        numTips = findViewById(R.id.numTips);
        mRecyclerView.addItemDecoration(new CustomDividerItemDecoration(AppConst.CustomDivider_BookList_RecyclerView_Color, AppConst.CustomDivider_Height));

        // 设置文本变化监听
        tipsBtnSearch.setOnClickListener(v -> {
            if (clearEditText.getText() != null) {
                setSearchText(clearEditText.getText() != null ? clearEditText.getText().toString() : "");
            }
        });
        clearEditText.addTextChangedListener(new TextWatcher() {

            private final Runnable runnable = () -> {
                String text = clearEditText.getText() != null ? clearEditText.getText().toString() : "";
                // 如果搜索文本为空，则重置匹配结果数量并刷新列表
                if (charSequenceIsEmpty(text)) {
                    reListAdapter(false);
                    numTips.setText("");
                } else {
                    setSearchText(text);
                }
            };

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                EasyLog.print("clearEditText", "onTextChanged: " + s);
                removeCallbacks(runnable);
                postDelayed(runnable, 300); // 延迟 300 毫秒执行
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

    }

    @SuppressLint("DefaultLocale")
    public void setSearchText(String searchText) {
        // this.searchText = searchText;
        // 重置匹配结果数量
        // 检查搜索文本是否有效（不为 null、不为空且不是数字）
        if (searchText != null && !searchText.isEmpty()) {
            searchTextResults.clear();
            for (String str : loadData()) {
                if (str.contains(searchText)) {
                    searchTextResults.add(str);
                }
            }
            if (this.numTips != null) {
                this.numTips.setText(String.format("%d个结果", searchTextResults.size()));
            }
            reListAdapter(true);
        } else {
            reListAdapter(false);
        }

    }

    /**
     * 重新设置适配器
     *
     * @param isSearch 是否搜索
     */
    @SuppressLint("NotifyDataSetChanged")
    private void reListAdapter(boolean isSearch) {
        if (isSearch) {
            mAdapter.setData(searchTextResults);
        } else {
            mAdapter.setData(loadData());
        }
        mAdapter.notifyDataSetChanged();
        if (loadData() != null && !loadData().isEmpty()) {
            ll_no_btn.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        } else {
            ll_no_btn.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
        }

    }

    /**
     * 判断 CharSequence 是否为空。
     *
     * @param charSequence 需要判断的 CharSequence
     * @return 如果为 null 或长度为 0，则返回 true；否则返回 false
     */
    public boolean charSequenceIsEmpty(CharSequence charSequence) {
        boolean bl = false;
        if (charSequence == null || charSequence.length() == 0) {
            //this.searchText = null;
            bl = true;
        }
        return bl;
    }

    private TipsFangYaoAdapter mAdapter = null;

    @Override
    protected void initData() {

        //判断是方或者药
        switch (typeFangYao) {
            case 1:
                mAdapter = new TipsFangYaoAdapter(getAttachActivity(), 1);
                break;
            case 2:
                mAdapter = new TipsFangYaoAdapter(getAttachActivity(), 2);
                break;
            case 3:
                mAdapter = new TipsFangYaoAdapter(getAttachActivity(), 3);
                break;
            default:
                break;
        }

        mRecyclerView.setAdapter(mAdapter);
        //设置数据源
        reListAdapter(false);
    }

    private List<String> loadData() {
        List<String> gvList;
        switch (typeFangYao) {
            case 1:
                // 获取当前书籍的方剂数据
                BookData bookData = BookDataManager.getInstance().getBookData(bookId);
                ChapterData fangData = bookData.getFangData();
                gvList = new ArrayList<>();
                if (fangData != null) {
                    List<DataItem> fangItems = fangData.getContent();
                    for (DataItem item : fangItems) {
                        gvList.add(item.getName());
                    }
                }
                break;
            case 2:
                // 获取所有药物（正名 + 别名）
                gvList = new ArrayList<>();
                GlobalDataHolder globalData = GlobalDataHolder.getInstance();
                
                // 添加正名
                if (globalData.getYaoMap() != null) {
                    gvList.addAll(globalData.getYaoMap().keySet());
                }
                
                // 添加别名
                if (globalData.getYaoAliasDict() != null) {
                    gvList.addAll(globalData.getYaoAliasDict().keySet());
                }
                break;
            default:
                String[] dataStrings = {"汉制一两约为 15.625克", "汉制一两为 24铢", "汉制一铢为 0.65克", "汉制一升约为 200毫升", "汉制一合为 20毫升", "杏仁一枚约为 0.4克", "1石=四钧＝29760克", "1钧=三十斤＝7440克", "1斤=248克", "1斤=16两", "1斤=液体250毫升", "1两=15.625克", "1两=24铢", "1升=液体200毫升", "1合=20毫升", "1圭=0.5克", "1龠=10毫升", "1撮=2克", "1方寸匕=金石类2.74克", "1方寸匕=药末约2克", "1方寸匕=草木类药末约1克", "半方寸匕=一刀圭=一钱匕=1.5克", "一钱匕=1.5-1.8克", "一铢=100个黍米的重量", "一分=3.9-4.2克", "梧桐子大约为 黄豆大", "蜀椒一升=50克", "葶力子一升=60克", "吴茱萸一升=50克", "五味子一升=50克", "半夏一升=130克", "虻虫一升=16克", "附子大者1枚=20-30克", "附子中者1枚=15克", "强乌头1枚小者=3克", "强乌头1枚大者=5-6克", "杏仁大者10枚=4克", "栀子10枚平均15克", "瓜蒌大小平均1枚=46克", "枳实1枚约14.4克", "石膏鸡蛋大1枚约40克", "厚朴1尺约30克", "竹叶一握约12克", "1斛=10斗＝20000毫升", "1斗=10升＝2000毫升", "1升=10合＝200毫升", "1合=2龠＝20毫升", "1龠=5撮＝10毫升", "1撮=4圭＝2毫升", "1圭=0.5毫升", "1引=10丈＝2310厘米", "1丈=10尺＝231厘米", "1尺=10寸＝23.1厘米", "1寸=10分＝2.31厘米", "1分=0.231厘米"};
                gvList = new ArrayList<>(Arrays.asList(dataStrings));
        }
        if (gvList != null && !gvList.isEmpty()) {
            return gvList;
        } else {
            return new ArrayList<>();
        }
    }
}