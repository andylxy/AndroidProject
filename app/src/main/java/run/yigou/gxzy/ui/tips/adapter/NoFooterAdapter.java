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
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.donkingliang.groupedadapter.adapter.GroupedRecyclerViewAdapter;
import com.donkingliang.groupedadapter.holder.BaseViewHolder;
import com.hjq.base.BaseDialog;
import com.hjq.http.EasyLog;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
        setLongClickForView(sectiontext, entity.getAttributed_child_sectiontext());
        setLongClickForView(sectionnote, entity.getAttributed_child_sectionnote());
        setLongClickForView(sectionvideo, entity.getAttributed_child_sectionvideo());
        // 为sectiontext设置点击监听，处理点击事件
        sectiontext.setOnClickListener(v -> {
            Boolean isClick = (Boolean) v.getTag();
            if (isClick != null && isClick) return;
            EasyLog.print("条文点击: " + v.getTag() + ", 实体信息: " + entity);
            toggleVisibility(sectionnote, entity.getChild_sectionnote());
        });

        // 为sectionnote设置点击监听，处理点击事件
        sectionnote.setOnClickListener(v -> {
            Boolean isClick = (Boolean) v.getTag();
            if (isClick != null && isClick) return;
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

   void setLongClickForView(TextView view, SpannableStringBuilder spannableString ){
       view.setOnLongClickListener(v -> {
           TipsNetHelper.initDialog(v.getContext());
           TipsNetHelper.menuDialogBuilder
                   .setListener((dialog, position, string) -> {
                       // 增加空值检查
                       Context context = v.getContext();
                       if (context != null) {
                           if (spannableString != null) {
                               TipsNetHelper.copyToClipboard(context, spannableString.toString());
                           } else {
                               // 可以记录日志或提示用户
                               EasyLog.print("CopyError", spannableString + " is null");
                           }
                       } else {
                           // 可以记录日志或提示用户
                           EasyLog.print("CopyError", "Context is null in " );
                       }

                   })
                   .show();
           return true;
       });
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
