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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.donkingliang.groupedadapter.adapter.GroupedRecyclerViewAdapter;
import com.donkingliang.groupedadapter.holder.BaseViewHolder;

import com.hjq.http.EasyLog;
import com.hjq.widget.layout.WrapRecyclerView;
import com.hjq.widget.view.ClearEditText;


import java.util.ArrayList;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.app.AppFragment;
import run.yigou.gxzy.common.AppConst;
import run.yigou.gxzy.ui.dividerItemdecoration.CustomDividerItemDecoration;
import run.yigou.gxzy.ui.tips.adapter.ExpandableAdapter;
import run.yigou.gxzy.ui.tips.entity.GroupModel;
import run.yigou.gxzy.ui.tips.entity.SearchKeyEntity;
import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;
import run.yigou.gxzy.ui.tips.tipsutils.SingletonNetData;
import run.yigou.gxzy.ui.tips.tipsutils.TipsNetHelper;
import run.yigou.gxzy.ui.tips.tipsutils.TipsSingleData;


public class TipsBookNetReadFragment extends AppFragment<AppActivity> {
    private WrapRecyclerView rvList;
    private ClearEditText clearEditText;
    private ExpandableAdapter adapter;
    private TextView numTips;
    private int bookId = 0;
    private String searchText = null;
    private Button tipsBtnSearch;

    private int showShanghan;
    private int showJinkui;
    LinearLayoutManager layoutManager;

    public TipsBookNetReadFragment() {
    }

    /**
     * @return
     */
    @Override
    protected int getLayoutId() {
        return R.layout.tips_book_read_activity_group_list;
    }

    SingletonNetData singletonNetData;

    /**
     *
     */
    @SuppressLint("CutPasteId")
    @Override
    protected void initView() {
        rvList = findViewById(R.id.tips_book_read_activity_group_list);
        layoutManager = new LinearLayoutManager(getContext());
        rvList.setLayoutManager(layoutManager);
        rvList.addItemDecoration(new CustomDividerItemDecoration());
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

                EasyLog.print("clearEditText", "onTextChanged: " + s);
                if (charSequenceIsEmpty(s)) {
                    reListAdapter(true, false);
                    numTips.setText("");
                } else {
                    setSearchText(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        // 注册事件
        // XEventBus.getDefault().register(this);
    }

    /**
     *
     */
    @Override
    protected void initData() {
        // 获取传递的书本编号
        Bundle args = getArguments();
        if (args != null) {
            bookId = args.getInt("bookNo", 0);
        }
        //获取指定书籍数据
        singletonNetData = TipsSingleData.getInstance().getMapBookContent(bookId);
        //兼容处理宋版伤寒,
        if (bookId == AppConst.ShangHanNo) {
            // 默认初始化设置  宋版伤寒,金匮显示
            // 从 SharedPreferences 中读取设置值
            SharedPreferences sharedPreferences = TipsSingleData.getInstance().getSharedPreferences();
            showShanghan = sharedPreferences.getInt(AppConst.Key_Shanghan, 0);
            showJinkui = sharedPreferences.getInt(AppConst.Key_Jinkui, 1);
            // 加载数据处理监听
            singletonNetData.setOnContentUpdateListener(new SingletonNetData.OnContentUpdateListener() {
                @Override
                public ArrayList<HH2SectionData> contentDateUpdate(ArrayList<HH2SectionData> contentList) {
                    if (contentList == null || contentList.isEmpty()) {
                        return new ArrayList<>();
                    }
                    int size = contentList.size();

                    int start = 0;
                    int end = size;

                    if (showJinkui == AppConst.Show_Jinkui_None) {
                        if (showShanghan == AppConst.Show_Shanghan_398) {
                            start = 8;
                            end = Math.min(18, size);
                        } else if (showShanghan == AppConst.Show_Shanghan_AllSongBan) {
                            end = Math.min(26, size);
                        }
                    } else if (showJinkui == AppConst.Show_Jinkui_Default) {
                        if (showShanghan == AppConst.Show_Shanghan_398) {
                            start = 8;
                        }
                    }

                    if (start < size) {
                        return new ArrayList<>(contentList.subList(start, end));
                    } else {
                        return  contentList;
                    }
                }
            });

            //宋版显示修改通知
            singletonNetData.setOnContentShowStatusNotification(new SingletonNetData.OnContentShowStatusNotification() {
                @Override
                public void contentShowStatusNotification(int status) {
                    //刷新数据显示
                    reListAdapter(true, false);
                }
            });
        }

        //加载到UI显示
        adapter = new ExpandableAdapter(getContext());

        reListAdapter(true, false);
        adapter.setOnHeaderClickListener(new GroupedRecyclerViewAdapter.OnHeaderClickListener() {
            @Override
            public void onHeaderClick(GroupedRecyclerViewAdapter adapter, BaseViewHolder holder,
                                      int groupPosition) {
//                Toast.makeText(getContext(), "组头：groupPosition = " + groupPosition,
//                        Toast.LENGTH_LONG).show();
                ExpandableAdapter expandableAdapter = (ExpandableAdapter) adapter;
                if (expandableAdapter.isExpand(groupPosition)) {
                    expandableAdapter.collapseGroup(groupPosition);
                } else {
                    expandableAdapter.expandGroup(groupPosition);
                }
            }
        });
        //跳转指定章节
        adapter.setOnJumpSpecifiedItemListener(new ExpandableAdapter.OnJumpSpecifiedItemListener() {
            @Override
            public void onJumpSpecifiedItem(int groupPosition, int childPosition) {
                //   reListAdapter(true, false);
                clearEditText.setText("");
                numTips.setText("");
                layoutManager.scrollToPositionWithOffset(groupPosition, 0);
                adapter.expandGroup(groupPosition, true);

            }
        });
        rvList.setAdapter(adapter);
    }




    @Override
    public void onDestroy() {
        super.onDestroy();
        singletonNetData.setOnContentShowStatusNotification(null);
        singletonNetData.setOnContentUpdateListener(null);
        adapter.setOnJumpSpecifiedItemListener(null);

    }


    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.tips_btn_search) {

            if (this.searchText == null) {
                reListAdapter(true, false);
            } else setSearchText(this.searchText);
        }
    }

    /**
     * @param init     true  初始化显示 ,false 搜索结果 显示
     * @param isExpand false 表头不展开, true 展开
     */
    private void reListAdapter(boolean init, boolean isExpand) {
        if (bookId != 0) {

            if (init) {
                adapter.setmGroups(GroupModel.getExpandableGroups(singletonNetData.getContent(), isExpand));
            } else {
                if (!singletonNetData.getSearchResList().isEmpty())
                    adapter.setmGroups(GroupModel.getExpandableGroups(singletonNetData.getSearchResList(), isExpand));
            }
            adapter.notifyDataChanged();
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
            SearchKeyEntity searchKeyEntity = new SearchKeyEntity(searchText);
            ArrayList<HH2SectionData> filteredData = TipsNetHelper.getSearchHh2SectionData(searchKeyEntity, singletonNetData);
            // 更新展示的数据和结果
            singletonNetData.setSearchResList(filteredData);
            if (this.numTips != null) {
                this.numTips.setText(String.format("%d个结果", searchKeyEntity.getSearchResTotalNum()));
            }
            if (this.adapter != null) {
                reListAdapter(false, true);
            }
        } else {
            if (this.adapter != null) {
                reListAdapter(true, false);
            }
        }

    }

    public static void start(Context context) {
        Intent intent = new Intent(context, TipsBookNetReadFragment.class);
        // intent.putExtra(APPCONST.BOOK, item);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

}
