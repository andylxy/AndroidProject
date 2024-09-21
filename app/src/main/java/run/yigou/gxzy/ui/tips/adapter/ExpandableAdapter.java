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

import android.content.Context;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import com.donkingliang.groupedadapter.adapter.GroupedRecyclerViewAdapter;
import com.donkingliang.groupedadapter.holder.BaseViewHolder;
import com.hjq.http.EasyLog;

import java.util.ArrayList;

import run.yigou.gxzy.R;
import run.yigou.gxzy.ui.tips.tipsutils.TipsNetHelper;
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
public class ExpandableAdapter extends GroupedRecyclerViewAdapter
        // implements View.OnLongClickListener ,View.OnClickListener
{

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

@Override
public void onBindChildViewHolder(BaseViewHolder holder, int groupPosition, int childPosition) {
    // 获取分组位置和子位置对应的实体对象
    ChildEntity entity = mGroups.get(groupPosition).getChildren().get(childPosition);
    // 从Holder中获取三个TextView
    TextView sectiontext = holder.get(R.id.tv_sectiontext);
    TextView sectionnote = holder.get(R.id.tv_sectionnote);
    TextView sectionvideo = holder.get(R.id.tv_sectionvideo);

    // 默认隐藏note和video的TextView
    sectionnote.setVisibility(View.GONE);
    sectionvideo.setVisibility(View.GONE);

    // 根据实体对象设置TextView的内容和点击方法
    if (entity.getChild_sectionnote() != null) {
        sectionnote.setText(entity.getAttributed_child_sectionnote());
        sectionnote.setMovementMethod(LocalLinkMovementMethod.getInstance());
    }
    if (entity.getChild_sectionvideo() != null) {
        sectionvideo.setText(entity.getAttributed_child_sectionvideo());
        sectionvideo.setMovementMethod(LocalLinkMovementMethod.getInstance());
    }

    // 设置sectiontext的文本内容
    sectiontext.setText(entity.getAttributed_child_sectiontext());
    sectiontext.setMovementMethod(LocalLinkMovementMethod.getInstance());

    // 为sectiontext设置长按监听，弹出复制对话框
    sectiontext.setOnLongClickListener(v -> {
        TipsNetHelper.initDialog(v.getContext());
        TipsNetHelper.menuDialogBuilder
                .setListener((dialog, position, string) -> {
                    TipsNetHelper.copyToClipboard(v.getContext(), entity.getSpannableChild().toString());
                })
                .show();
        return true;
    });

    // 为sectiontext设置点击监听，处理点击事件
    sectiontext.setOnClickListener(v -> {
        Object isClick = v.getTag();
        if (isClick != null && (boolean) isClick) return;
        EasyLog.print("条文点击:" + v.getTag());
        toggleVisibility(sectionnote, entity.getChild_sectionnote());
    });

    // 为sectionnote设置点击监听，处理点击事件
    sectionnote.setOnClickListener(v -> {
        Object isClick = v.getTag();
        if (isClick != null && (boolean) isClick) return;
        toggleVisibility(sectionvideo, entity.getChild_sectionvideo());
    });

    // 为sectionvideo设置点击监听，处理点击事件
    sectionvideo.setOnClickListener(v -> {
        toggleVisibility(sectionvideo, entity.getChild_sectionvideo());
    });
}
private void toggleVisibility(TextView textView, String content) {
    if (textView.getVisibility() == View.VISIBLE) {
        textView.setVisibility(View.GONE);
    } else if (content != null) {
        textView.setVisibility(View.VISIBLE);
    }
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
