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
import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import com.donkingliang.groupedadapter.adapter.GroupedRecyclerViewAdapter;
import com.donkingliang.groupedadapter.holder.BaseViewHolder;
import com.hjq.http.EasyLog;

import run.yigou.gxzy.action.ToastAction;

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
public class ExpandableAdapter extends GroupedRecyclerViewAdapter implements ToastAction
        // implements View.OnLongClickListener ,View.OnClickListener
{

    public ArrayList<ExpandableGroupEntity> getmGroups() {
        return mGroups;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setmGroups(ArrayList<ExpandableGroupEntity> mGroups) {
        this.mGroups = mGroups;
        // notifyDataSetChanged();
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
        if (entity.getAttributed_child_sectionnote() != null) {
            sectionnote.setText(entity.getAttributed_child_sectionnote());
            sectionnote.setMovementMethod(LocalLinkMovementMethod.getInstance());
        }
        if (entity.getAttributed_child_sectionvideo() != null) {
            sectionvideo.setText(entity.getAttributed_child_sectionvideo());
            sectionvideo.setMovementMethod(LocalLinkMovementMethod.getInstance());
        }

        // 设置sectiontext的文本内容
        sectiontext.setText(entity.getAttributed_child_sectiontext());
        sectiontext.setMovementMethod(LocalLinkMovementMethod.getInstance());
        // 设置长按监听，弹出复制对话框
        setLongClickForView(sectiontext, entity.getAttributed_child_sectiontext(), entity.getGroupPosition());
        setLongClickForView(sectionnote, entity.getAttributed_child_sectionnote(), entity.getGroupPosition());
        setLongClickForView(sectionvideo, entity.getAttributed_child_sectionvideo(), entity.getGroupPosition());

        // 为sectiontext设置点击监听，处理点击事件
        sectiontext.setOnClickListener(v -> {
            Boolean isClick = (Boolean) v.getTag();
            if (isClick != null && isClick) return;
            EasyLog.print("条文点击: " + v.getTag() + ", 实体信息: " + entity);
            toggleVisibility(sectionnote, entity.getAttributed_child_sectionnote());
        });

        // 为sectionnote设置点击监听，处理点击事件
        sectionnote.setOnClickListener(v -> {
            Boolean isClick = (Boolean) v.getTag();
            if (isClick != null && isClick) return;
            toggleVisibility(sectionvideo, entity.getAttributed_child_sectionvideo());
        });

        // 为sectionvideo设置点击监听，处理点击事件
        sectionvideo.setOnClickListener(v -> {
            Boolean isClick = (Boolean) v.getTag();
            if (isClick != null && isClick) return;
            toggleVisibility(sectionvideo, entity.getAttributed_child_sectionvideo());
        });

    }

    private void toggleVisibility(TextView textView, SpannableStringBuilder content) {
        // 增加对content的空值检查
        if (content == null) {
            return;
        }

        String contentString = content.toString();

        // 简化逻辑
        if (textView.getVisibility() == View.VISIBLE) {
            textView.setVisibility(View.GONE);
        } else if (!contentString.isEmpty()) { // 判断是否为空字符串
            textView.setVisibility(View.VISIBLE);
        }
    }


    void setLongClickForView(TextView view, SpannableStringBuilder spannableString, int groupPosition) {
        view.setOnLongClickListener(v -> {

            TipsNetHelper.showListDialog(v.getContext())
                    .setListener((dialog, position, string) -> {
                        Context context = v.getContext();
                        if (string.equals("拷贝内容")) {
                            if (context != null) {
                                if (spannableString != null) {
                                    TipsNetHelper.copyToClipboard(context, spannableString.toString());
                                } else {
                                    EasyLog.print("CopyError", "spannableString is null");
                                    try {
                                        toast("内容为空，无法拷贝");
                                    } catch (Exception e) {
                                        EasyLog.print("CopyError", "Failed to show Toast: " + e.getMessage());
                                    }
                                }
                            } else {
                                EasyLog.print("CopyError", "Context is null");
                                try {
                                    toast("上下文无效，无法拷贝");
                                } catch (Exception e) {
                                    EasyLog.print("CopyError", "Failed to show Toast: " + e.getMessage());
                                }
                            }
                        } else if (string.equals("跳转到本章内容")) {
                            if (mOnJumpSpecifiedItemListener != null && groupPosition > 0) {
                                mOnJumpSpecifiedItemListener.onJumpSpecifiedItem(groupPosition, -1);
                            }
                        }
                    })
                    .show();

            return true;
        });
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

    private OnJumpSpecifiedItemListener mOnJumpSpecifiedItemListener;

    public interface OnJumpSpecifiedItemListener {
        void onJumpSpecifiedItem(int groupPosition, int childPosition);
    }

    /**
     * 设置子项长按事件
     *
     * @param listener
     */
    public void setOnJumpSpecifiedItemListener(OnJumpSpecifiedItemListener listener) {
        mOnJumpSpecifiedItemListener = listener;
    }

}
