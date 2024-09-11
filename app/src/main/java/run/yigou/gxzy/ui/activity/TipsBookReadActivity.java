/*
 * 项目名: AndroidProject
 * 类名: TipsBookReadActivity.java
 * 包名: run.yigou.gxzy.ui.activity.TipsBookReadActivity
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月08日 10:50:43
 * 上次修改时间: 2024年09月08日 10:50:43
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.donkingliang.groupedadapter.adapter.GroupedRecyclerViewAdapter;
import com.donkingliang.groupedadapter.holder.BaseViewHolder;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.tipsutils.SingletonData;
import run.yigou.gxzy.ui.tips.ExpandableAdapter;
import run.yigou.gxzy.ui.tips.GroupModel;

public class TipsBookReadActivity extends AppActivity {
    private RecyclerView rvList;
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
        rvList.setLayoutManager(new LinearLayoutManager(this));
        ExpandableAdapter adapter = new ExpandableAdapter(this, GroupModel.getExpandableGroups(SingletonData.getInstance(),false));

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
//    @NonNull
//    @Override
//    protected ImmersionBar createStatusBarConfig() {
//        return super.createStatusBarConfig()
//                // 指定导航栏背景颜色
//                .navigationBarColor(R.color.white);
//    }
    public static void start(Context context) {
        Intent intent = new Intent(context, TipsBookReadActivity.class);
       // intent.putExtra(APPCONST.BOOK, item);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    @Override
    public boolean isStatusBarEnabled() {
        // 使用沉浸式状态栏
        return !super.isStatusBarEnabled();
    }
}
