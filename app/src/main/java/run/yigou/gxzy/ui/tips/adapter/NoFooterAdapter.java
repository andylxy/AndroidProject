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
import android.view.View;
import android.widget.TextView;

import com.donkingliang.groupedadapter.adapter.GroupedRecyclerViewAdapter;
import com.donkingliang.groupedadapter.holder.BaseViewHolder;
import com.hjq.base.BaseDialog;
import com.hjq.http.EasyLog;

import java.util.ArrayList;

import run.yigou.gxzy.R;
import run.yigou.gxzy.ui.dialog.MenuDialog;
import run.yigou.gxzy.ui.tips.tipsutils.TipsNetHelper;
import run.yigou.gxzy.ui.tips.widget.LocalLinkMovementMethod;
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

    public NoFooterAdapter(Context context) {
        super(context);
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



//    @Override
//    public void onBindChildViewHolder(BaseViewHolder holder, int groupPosition, int childPosition) {
//        ChildEntity entity = mGroups.get(groupPosition).getChildren().get(childPosition);
//        TextView textView= holder.get(R.id.tv_sectiontext);
//        SpannableStringBuilder renderText =  TipsNetHelper.renderText(entity.getChild_sectiontext());
//        textView.setText(renderText);
//        textView.setMovementMethod(LocalLinkMovementMethod.getInstance());
//        //长按弹出复制
//        textView.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View v) {
//                // 底部选择框
//                TipsNetHelper.initDialog(v.getContext());
//                TipsNetHelper.menuDialogBuilder
//                        .setListener(new MenuDialog.OnListener<String>() {
//                            @Override
//                            public void onSelected(BaseDialog dialog, int position, String string) {
//                                //Toast.makeText(v.getContext(), "位置：" + position + "，文本：" + string, Toast.LENGTH_LONG).show();
//                                // 复制到剪贴板
//                                TipsNetHelper. copyToClipboard(v.getContext(), renderText.toString());
//                            }
//
//                            // @Override
//                            // public void onCancel(BaseDialog dialog) {
//                            //Toast.makeText(v.getContext(), "取消了", Toast.LENGTH_LONG).show();
//                            // }
//                        })
//                        .show();
//                // 返回 true 表示事件已被处理
//                return true;
//            }
//        });
//    }
//
//



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
