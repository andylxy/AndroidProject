/*
 * 项目名: AndroidProject
 * 类名: ExpandableAdapter.java
 * 包名: run.yigou.gxzy.ui.tips.adapter.ExpandableAdapter
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月12日 09:24:05
 * 上次修改时间: 2024年09月11日 23:04:08
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import com.donkingliang.groupedadapter.adapter.GroupedRecyclerViewAdapter;
import com.donkingliang.groupedadapter.holder.BaseViewHolder;

import java.util.ArrayList;

import run.yigou.gxzy.R;
import run.yigou.gxzy.ui.tips.widget.LocalLinkMovementMethod;
import run.yigou.gxzy.ui.tips.entity.ChildEntity;
import run.yigou.gxzy.ui.tips.entity.ExpandableGroupEntity;

/**
 * 可展开收起的Adapter。他跟普通的{/@link GroupedListAdapter}基本是一样的。
 * 它只是利用了{@link GroupedRecyclerViewAdapter}的
 * 删除一组里的所有子项{@link GroupedRecyclerViewAdapter#notifyChildrenRemoved(int)}} 和
 * 插入一组里的所有子项{@link GroupedRecyclerViewAdapter#notifyChildrenInserted(int)}
 * 两个方法达到列表的展开和收起的效果。
 * 这种列表类似于{@link ExpandableListView}的效果。
 * 这里我把列表的组尾去掉是为了效果上更像ExpandableListView。
 */
public class ExpandableAdapter extends GroupedRecyclerViewAdapter {

    public ArrayList<ExpandableGroupEntity> getmGroups() {
        return mGroups;
    }

    public void setmGroups(ArrayList<ExpandableGroupEntity> mGroups) {
        this.mGroups = mGroups;
    }

    private ArrayList<ExpandableGroupEntity> mGroups;

    public ExpandableAdapter(Context context) {
        super(context);
    }

    @Override
    public int getGroupCount() {
        return mGroups == null ? 0 : mGroups.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        //如果当前组收起，就直接返回0，否则才返回子项数。这是实现列表展开和收起的关键。
        if (!isExpand(groupPosition)) {
            return 0;
        }
        ArrayList<ChildEntity> children = mGroups.get(groupPosition).getChildren();
        return children == null ? 0 : children.size();
    }

    @Override
    public boolean hasHeader(int groupPosition) {
        return true;
    }

    @Override
    public boolean hasFooter(int groupPosition) {
        return false;
    }

    @Override
    public int getHeaderLayout(int viewType) {
        return R.layout.adapter_expandable_header;
    }

    @Override
    public int getFooterLayout(int viewType) {
        return 0;
    }

    @Override
    public int getChildLayout(int viewType) {
        return R.layout.adapter_child;
    }

    @Override
    public void onBindHeaderViewHolder(BaseViewHolder holder, int groupPosition) {
        ExpandableGroupEntity entity = mGroups.get(groupPosition);
        //holder.setText(R.id.tv_expandable_header, entity.getHeader());
        TextView textView = holder.get(R.id.tv_expandable_header);
       // SpannableStringBuilder renderText = TipsHelper.renderText(entity.getHeader());
        textView.setText(entity.getSpannableHeader());
        ImageView ivState = holder.get(R.id.iv_state);
        if (entity.isExpand()) {
            ivState.setRotation(90);
        } else {
            ivState.setRotation(0);
        }
    }

    @Override
    public void onBindFooterViewHolder(BaseViewHolder holder, int groupPosition) {
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindChildViewHolder(BaseViewHolder holder, int groupPosition, int childPosition) {
        ChildEntity entity = mGroups.get(groupPosition).getChildren().get(childPosition);
        TextView textView = holder.get(R.id.tv_child);
        //SpannableStringBuilder renderText = Helper.renderText(entity.getChild());
        textView.setText(entity.getSpannableChild());
        textView.setMovementMethod(LocalLinkMovementMethod.getInstance());
//        textView.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                mRawX = event.getRawX();
//                mRawY = event.getRawY();
//                v.setTag(event);
//                Log.e("ExpandableAdapter", "可点击的文本 (按下或抬起) mRawX : "+event.getRawX() +" mRawY: "+event.getRawY());
//                return false;
//            }
//        });
    }

    /**
     * 判断当前组是否展开
     *
     * @param groupPosition
     * @return
     */
    public boolean isExpand(int groupPosition) {
        ExpandableGroupEntity entity = mGroups.get(groupPosition);
        return entity.isExpand();
    }

    /**
     * 展开一个组
     *
     * @param groupPosition
     */
    public void expandGroup(int groupPosition) {
        expandGroup(groupPosition, false);
    }

    /**
     * 展开一个组
     *
     * @param groupPosition
     * @param animate
     */
    public void expandGroup(int groupPosition, boolean animate) {
        ExpandableGroupEntity entity = mGroups.get(groupPosition);
        entity.setExpand(true);
        if (animate) {
            notifyChildrenInserted(groupPosition);
        } else {
            notifyDataChanged();
        }
    }

    /**
     * 收起一个组
     *
     * @param groupPosition
     */
    public void collapseGroup(int groupPosition) {
        collapseGroup(groupPosition, false);
    }

    /**
     * 收起一个组
     *
     * @param groupPosition
     * @param animate
     */
    public void collapseGroup(int groupPosition, boolean animate) {
        ExpandableGroupEntity entity = mGroups.get(groupPosition);
        entity.setExpand(false);
        if (animate) {
            notifyChildrenRemoved(groupPosition);
        } else {
            notifyDataChanged();
        }
    }
}
