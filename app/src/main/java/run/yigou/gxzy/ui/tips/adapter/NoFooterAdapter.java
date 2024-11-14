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
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.donkingliang.groupedadapter.adapter.GroupedRecyclerViewAdapter;
import com.donkingliang.groupedadapter.holder.BaseViewHolder;
import com.hjq.http.EasyLog;

import java.util.ArrayList;

import run.yigou.gxzy.R;
import run.yigou.gxzy.common.AppConst;
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
        if (entity.getAttributed_child_section_note() != null) {
            sectionnote.setText(entity.getAttributed_child_section_note());
            sectionnote.setMovementMethod(LocalLinkMovementMethod.getInstance());
        }
        if (entity.getAttributed_child_section_video() != null) {
            sectionvideo.setText(entity.getAttributed_child_section_video());
            sectionvideo.setMovementMethod(LocalLinkMovementMethod.getInstance());
        }
        //包含文本的SpannableString
        SpannableStringBuilder spannableString = entity.getAttributed_child_section_text();
        // 设置sectiontext的文本内容
        if (entity.getChild_section_image() == null) {
            sectiontext.setText(spannableString);
        } else {
            // 使用 Glide 加载图片并添加到SpannableString
            String imageUrl = AppConst.ImageHost+ entity.getChild_section_image();
            Glide.with(sectiontext.getContext())
                    .load(imageUrl)
                    .into(new CustomTarget<Drawable>() {
                        /**
                         * @param resource   the loaded resource.
                         * @param transition
                         */
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            // 图片加载完成后，设置图片到指定位置
                            resource.setBounds(0, 0, resource.getIntrinsicWidth(), resource.getIntrinsicHeight());

                            // 将图片插入到指定位置
                            ImageSpan imageSpan = new ImageSpan(resource, ImageSpan.ALIGN_BASELINE);
                            spannableString.setSpan(imageSpan, 0, 2, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);

                            // 将处理好的SpannableString设置到TextView
                            sectiontext.setText(spannableString);

                            // 获取 TextView 的宽度
                            sectiontext.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                                @Override
                                public boolean onPreDraw() {
                                    // 获取TextView的宽度
                                    int textViewWidth = sectiontext.getWidth();

                                    // 如果宽度有效且图片已加载完成
                                    if (textViewWidth > 0) {
                                        // 获取图片的原始宽高
                                        int originalWidth = resource.getIntrinsicWidth();
                                        int originalHeight = resource.getIntrinsicHeight();

                                        // 根据 TextView 宽度调整图片大小，保持比例
                                        float ratio = (float) originalWidth / originalHeight;
                                        int imageWidth = (int) ((int) textViewWidth*0.90f);
                                        int imageHeight = (int) (imageWidth / ratio);

                                        // 设置图片的大小
                                        resource.setBounds(0, 0, imageWidth, imageHeight);

                                        // 将图片插入到 SpannableString 中
                                        ImageSpan imageSpan = new ImageSpan(resource, ImageSpan.ALIGN_BASELINE);
                                        spannableString.setSpan(imageSpan, 0, 2, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
                                        // 在图片后添加换行符
                                        spannableString.append("\n");
                                        // 将处理好的 SpannableString 设置到 TextView
                                        sectiontext.setText(spannableString);

                                        // 移除监听器，避免重复绘制
                                        sectiontext.getViewTreeObserver().removeOnPreDrawListener(sectiontext);
                                    }

                                    return true;
                                }
                            });
                        }

                        @Override
                        public void onLoadCleared(Drawable placeholder) {
                            // 可以在图片加载失败时显示一个占位符，或者做其他处理
                        }
                    });

        }

        sectiontext.setMovementMethod(LocalLinkMovementMethod.getInstance());
        setLongClickForView(sectiontext, entity.getAttributed_child_section_text());
        setLongClickForView(sectionnote, entity.getAttributed_child_section_note());
        setLongClickForView(sectionvideo, entity.getAttributed_child_section_video());
        // 为sectiontext设置点击监听，处理点击事件
        sectiontext.setOnClickListener(v -> {
            Boolean isClick = (Boolean) v.getTag();
            if (isClick != null && isClick) return;
            // EasyLog.print("条文点击: " + v.getTag() + ", 实体信息: " + entity);
            toggleVisibility(sectionnote, entity.getAttributed_child_section_note());
        });

        // 为sectionnote设置点击监听，处理点击事件
        sectionnote.setOnClickListener(v -> {
            Boolean isClick = (Boolean) v.getTag();
            if (isClick != null && isClick) return;
            toggleVisibility(sectionvideo, entity.getAttributed_child_section_video());
        });

        // 为sectionvideo设置点击监听，处理点击事件
        sectionvideo.setOnClickListener(v -> {
            Boolean isClick = (Boolean) v.getTag();
            if (isClick != null && isClick) return;
            toggleVisibility(sectionvideo, entity.getAttributed_child_section_video());
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

    void setLongClickForView(TextView view, SpannableStringBuilder spannableString) {
        view.setOnLongClickListener(v -> {

            TipsNetHelper.showListDialog(v.getContext())
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
                            EasyLog.print("CopyError", "Context is null in ");
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
