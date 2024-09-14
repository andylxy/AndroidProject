/*
 * 项目名: AndroidProject
 * 类名: TipsBookReadActivity.java
 * 包名: run.yigou.gxzy.ui.activity.TipsBookReadActivity
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月08日 10:50:43
 * 上次修改时间: 2024年09月08日 10:50:43
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.donkingliang.groupedadapter.adapter.GroupedRecyclerViewAdapter;
import com.donkingliang.groupedadapter.holder.BaseViewHolder;
import com.hjq.widget.view.ClearEditText;

import java.util.ArrayList;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.app.AppFragment;
import run.yigou.gxzy.ui.activity.TipsFragmentActivity;
import run.yigou.gxzy.ui.tips.adapter.ExpandableAdapter;
import run.yigou.gxzy.ui.tips.entity.GroupModel;
import run.yigou.gxzy.ui.tips.entity.SearchKeyEntity;
import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;
import run.yigou.gxzy.ui.tips.tipsutils.SingletonData;
import run.yigou.gxzy.ui.tips.tipsutils.TipsHelper;


public class TipsBookReadFragment extends AppFragment<AppActivity> {
    private RecyclerView rvList;
    private ClearEditText clearEditText;
    private ExpandableAdapter adapter;
    private TextView numTips;
   // private int totalNum;
    private String searchText = null;
    private Button tipsBtnSearch;

    public TipsBookReadFragment(){}
    /**
     * @return
     */
    @Override
    protected int getLayoutId() {
        return R.layout.tips_book_read_activity_group_list;
    }

    /**
     *
     */
    @Override
    protected void initView() {
        rvList = findViewById(R.id.tips_book_read_activity_group_list);
        rvList.setLayoutManager(new LinearLayoutManager(getContext()));
        clearEditText = findViewById(R.id.include_tips_book_read).findViewById(R.id.searchEditText);
        tipsBtnSearch = findViewById(R.id.include_tips_book_read).findViewById(R.id.tips_btn_search);
        tipsBtnSearch.setOnClickListener(this);
        numTips = findViewById(R.id.include_tips_book_read).findViewById(R.id.numTips);
        clearEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                Log.d("clearEditText", "onTextChanged: " + s);
                if (charSequenceIsEmpty(s)) {
                    reListAdapter(true, false);
                    numTips.setText("");
                    Log.d("clearEditText", "onTextChanged: 1");

                } else {
                    setSearchText(s.toString());
                    Log.d("clearEditText", "onTextChanged: 2");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        adapter = new ExpandableAdapter(getContext());
        reListAdapter(true, false);
        adapter.setOnHeaderClickListener(new GroupedRecyclerViewAdapter.OnHeaderClickListener() {
            @Override
            public void onHeaderClick(GroupedRecyclerViewAdapter adapter, BaseViewHolder holder,
                                      int groupPosition) {
//                Toast.makeText(ExpandableActivity.this, "组头：groupPosition = " + groupPosition,
//                        Toast.LENGTH_LONG).show();
                ExpandableAdapter expandableAdapter = (ExpandableAdapter) adapter;
                if (expandableAdapter.isExpand(groupPosition)) {
                    expandableAdapter.collapseGroup(groupPosition);
                } else {
                    expandableAdapter.expandGroup(groupPosition);
                }
            }
        });
        adapter.setOnChildClickListener(new GroupedRecyclerViewAdapter.OnChildClickListener() {
            @Override
            public void onChildClick(GroupedRecyclerViewAdapter adapter, BaseViewHolder holder,
                                     int groupPosition, int childPosition) {
//                Toast.makeText(getContext(), "子项：groupPosition = " + groupPosition
//                                + ", childPosition = " + childPosition,
//                        Toast.LENGTH_LONG).show();
                //String text=  ((TextView) holder.get(R.id.tv_child2)).getText().toString();
                //Toast.makeText(getContext(), text,Toast.LENGTH_LONG).show();

            }
        });
        rvList.setAdapter(adapter);
    }

    /**
     *
     */
    @Override
    protected void initData() {

    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.tips_btn_search) {

            if (this.searchText == null) {
                reListAdapter(true,false);
            } else setSearchText(this.searchText);
        }
    }

    /**
     * @param init    true  初始化显示 ,false 搜索结果 显示
     * @param isExpand false 表头不展开, true 展开
     */
    private void reListAdapter(boolean init, boolean isExpand) {
        if (init) {
            adapter.setmGroups(GroupModel.getExpandableGroups(SingletonData.getInstance().getContent(), isExpand,false));
        } else {
            if (SingletonData.getInstance().getSearchResList() != null)
                adapter.setmGroups(GroupModel.getExpandableGroups(SingletonData.getInstance().getSearchResList(), isExpand,true));
        }
        adapter.notifyDataChanged();
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
            this.searchText = null;
            bl = true;
        }
        return bl;
    }

    @SuppressLint("DefaultLocale")
    public void setSearchText(String searchText) {
        this.searchText = searchText;
        // 重置匹配结果数量
        // 检查搜索文本是否有效（不为 null、不为空且不是数字）
        if (searchText != null && !searchText.isEmpty() /*&& !TipsHelper.isNumeric(searchText)*/) {
            SearchKeyEntity searchKeyEntity  = new SearchKeyEntity(searchText);
            ArrayList<HH2SectionData> filteredData = TipsHelper.getSearchHh2SectionData(searchKeyEntity);
            // 更新展示的数据和结果
            SingletonData.getInstance().setSearchResList(filteredData);
            if (this.numTips != null) {
                this.numTips.setText(String.format("%d个结果", searchKeyEntity.getSearchResTotalNum()));
            }
            if (this.adapter != null) {
                reListAdapter(false,true);
            }
        } else {
            if (this.adapter != null) {
                reListAdapter(true, false);
            }
        }

    }


    //    @NonNull
//    @Override
//    protected ImmersionBar createStatusBarConfig() {
//        return super.createStatusBarConfig()
//                // 指定导航栏背景颜色
//                .navigationBarColor(R.color.white);
//    }
    public static void start(Context context) {
        Intent intent = new Intent(context, TipsBookReadFragment.class);
        // intent.putExtra(APPCONST.BOOK, item);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }



}
