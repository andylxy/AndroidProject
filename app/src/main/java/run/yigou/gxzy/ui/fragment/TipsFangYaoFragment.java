package run.yigou.gxzy.ui.fragment;

import android.annotation.SuppressLint;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.TextView;

import com.hjq.http.EasyLog;
import com.hjq.widget.layout.WrapRecyclerView;
import com.hjq.widget.view.ClearEditText;

import java.util.ArrayList;
import java.util.List;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.app.TitleBarFragment;
import run.yigou.gxzy.common.AppConst;
import run.yigou.gxzy.ui.adapter.TipsFangYaoAdapter;
import run.yigou.gxzy.ui.dividerItemdecoration.CustomDividerItemDecoration;
import run.yigou.gxzy.ui.tips.tipsutils.TipsSingleData;


public final class TipsFangYaoFragment extends TitleBarFragment<AppActivity> {

    public static final String TAG = "TipsFangYaoFragment";
    private boolean isFangYao;
    private TextView numTips;
    private WrapRecyclerView mRecyclerView;
   // private String searchText = null;
    private Button tipsBtnSearch;
    private ClearEditText clearEditText;

    // 使用 for 循环查找包含 SearchText控件指定内容
    List<String> searchTextResults = new ArrayList<>();

    public static TipsFangYaoFragment newInstance(boolean isFang) {
        TipsFangYaoFragment fragment = new TipsFangYaoFragment();
        fragment.isFangYao = isFang;
        return fragment;
    }


    @Override
    protected int getLayoutId() {
        return R.layout.tips_fragment_fang_yao;
    }

    @Override
    protected void initView() {

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
        if (searchText != null && !searchText.isEmpty() ) {
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
        if (isFangYao)
            mAdapter = new TipsFangYaoAdapter(getAttachActivity(), true);
        else
            mAdapter = new TipsFangYaoAdapter(getAttachActivity(), false);
        //mAdapter.setData(loadData());
        mRecyclerView.setAdapter(mAdapter);
        //设置数据源
        reListAdapter(false);
    }

    private List<String> loadData() {
        if (isFangYao) {
            return TipsSingleData.getInstance().getCurSingletonData().getAllFang();
        } else {
            return TipsSingleData.getInstance().getAllYao();
        }

    }
}