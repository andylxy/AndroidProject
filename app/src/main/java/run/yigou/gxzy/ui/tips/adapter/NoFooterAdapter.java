/*
 * 项目名: AndroidProject
 * 类名: NoFooterAdapter.java
 * 包名: run.yigou.gxzy.ui.tips.adapter.NoFooterAdapter
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月12日 09:24:05
 * 上次修改时间: 2024年09月11日 08:27:50
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips.adapter;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.widget.TextView;

import com.donkingliang.groupedadapter.adapter.GroupedRecyclerViewAdapter;
import com.donkingliang.groupedadapter.holder.BaseViewHolder;

import java.util.ArrayList;

import run.yigou.gxzy.R;
import run.yigou.gxzy.ui.tips.tipsutils.DataBeans.LocalLinkMovementMethod;
import run.yigou.gxzy.ui.tips.tipsutils.TipsHelper;
import run.yigou.gxzy.ui.tips.entity.ChildEntity;
import run.yigou.gxzy.ui.tips.entity.GroupEntity;

/**
 * 这是不带组尾的Adapter。
 * 只需要{@link GroupedRecyclerViewAdapter#hasFooter(int)}方法返回false就可以去掉组尾了。
 */
public class NoFooterAdapter extends GroupedListAdapter {

    public NoFooterAdapter(Context context, ArrayList<GroupEntity> groups) {
        super(context, groups);
    }
    @Override
    public void onBindChildViewHolder(BaseViewHolder holder, int groupPosition, int childPosition) {
        ChildEntity entity = mGroups.get(groupPosition).getChildren().get(childPosition);
        TextView textView= holder.get(R.id.tv_child);
        SpannableStringBuilder renderText =  TipsHelper.renderText(entity.getChild());
        textView.setText(renderText);
        textView.setMovementMethod(LocalLinkMovementMethod.getInstance());

    }
    /**
     * 返回false表示没有组尾
     *
     * @param groupPosition
     * @return
     */
    @Override
    public boolean hasFooter(int groupPosition) {
        return false;
    }

    /**
     * 当hasFooter返回false时，这个方法不会被调用。
     *
     * @return
     */
    @Override
    public int getFooterLayout(int viewType) {
        return 0;
    }

    /**
     * 当hasFooter返回false时，这个方法不会被调用。
     *
     * @param holder
     * @param groupPosition
     */
    @Override
    public void onBindFooterViewHolder(BaseViewHolder holder, int groupPosition) {

    }

}
