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
import run.yigou.gxzy.other.AppConfig;
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
        EasyLog.print("=== NoFooterAdapter构造函数 ===");
        EasyLog.print("接收到groups: " + (groups != null ? groups.size() : "null"));
        if (groups != null) {
            for (int i = 0; i < groups.size(); i++) {
                GroupEntity g = groups.get(i);
                EasyLog.print("  Group[" + i + "]: header=" + g.getHeader() + ", children=" + (g.getChildren() != null ? g.getChildren().size() : "null"));
            }
        }
        EasyLog.print("getItemCount(): " + getItemCount());
    }

    @Override
    public void onBindChildViewHolder(BaseViewHolder holder, int groupPosition, int childPosition) {
        // 获取分组位置和子位置对应的实体对象
        ChildEntity entity = mGroups.get(groupPosition).getChildren().get(childPosition);
        // 从Holder中获取三个TextView
        TextView section_text = holder.get(R.id.tv_sectiontext);
        TextView section_note = holder.get(R.id.tv_sectionnote);
        TextView section_video = holder.get(R.id.tv_sectionvideo);

        // 默认隐藏note和video的TextView
        section_note.setVisibility(View.GONE);
        section_video.setVisibility(View.GONE);

        // 根据实体对象设置TextView的内容和点击方法
        if (entity.getAttributed_child_section_note() != null && entity.getAttributed_child_section_note().length() > 0) {
            section_note.setText(entity.getAttributed_child_section_note());
            section_note.setMovementMethod(LocalLinkMovementMethod.getInstance());
        }
        if (entity.getAttributed_child_section_video() != null && entity.getAttributed_child_section_video().length() > 0) {
            section_video.setText(entity.getAttributed_child_section_video());
            section_video.setMovementMethod(LocalLinkMovementMethod.getInstance());
        }
        //包含文本的SpannableString
        SpannableStringBuilder spannableString = new SpannableStringBuilder();
        spannableString.append("12");
        // 设置sectiontext的文本内容
        if (entity.getChild_section_image() == null) {
            spannableString.clear();
            spannableString.append(entity.getAttributed_child_section_text());
            section_text.setText(spannableString);
        } else {

            spannableString.append(entity.getAttributed_child_section_text());
            // 使用 Glide 加载图片并添加到SpannableString
            StringBuilder imageUrl = new StringBuilder();
            if (AppConfig.isLogEnable()) {
                imageUrl.append(AppConfig.getHostUrl()).append(entity.getChild_section_image());
            } else {
                imageUrl.append(AppConst.ImageHost).append(entity.getChild_section_image());
            }
           // String url = imageUrl.toString();
            Glide.with(section_text.getContext())
                    .load( imageUrl.toString())
                    .into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            // 图片加载完成后，设置图片的初始大小
                            resource.setBounds(0, 0, resource.getIntrinsicWidth(), resource.getIntrinsicHeight());

                            // 将图片插入到指定位置
                            ImageSpan imageSpan = new ImageSpan(resource, ImageSpan.ALIGN_BASELINE);
                            spannableString.setSpan(imageSpan, 0, 2, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);

                            // 设置初始文本和图片到 TextView
                            section_text.setText(spannableString);

                            // 获取 TextView 的宽度并调整图片的大小
                            section_text.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                                @Override
                                public boolean onPreDraw() {
                                    // 获取 TextView 的宽度
                                    int textViewWidth = section_text.getWidth();

                                    if (textViewWidth > 0) {
                                        // 获取图片的原始宽高
                                        int originalWidth = resource.getIntrinsicWidth();
                                        int originalHeight = resource.getIntrinsicHeight();

                                        // 根据 TextView 宽度调整图片大小，保持比例
                                        float ratio = (float) originalWidth / originalHeight;
                                        int imageWidth = (int) (textViewWidth * 0.90f); // 图片宽度为 TextView 宽度的 90%
                                        int imageHeight = (int) (imageWidth / ratio);

                                        // 设置图片的大小
                                        resource.setBounds(0, 0, imageWidth, imageHeight);

                                        // 更新 ImageSpan，并重新设置到 SpannableString
                                        ImageSpan newImageSpan = new ImageSpan(resource, ImageSpan.ALIGN_BASELINE);
                                        spannableString.setSpan(newImageSpan, 0, 2, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);

                                        // 在图片后添加换行符
                                        spannableString.append("\n");

                                        // 设置处理好的 SpannableString 到 TextView
                                        section_text.setText(spannableString);

                                        // 强制重新布局，更新 TextView 高度
                                        section_text.requestLayout();

                                        // 移除监听器，避免重复绘制
                                        section_text.getViewTreeObserver().removeOnPreDrawListener(this);
                                    }

                                    return true;  // 返回 true 表示继续绘制
                                }
                            });
                        }

                        @Override
                        public void onLoadCleared(Drawable placeholder) {
                            // 图片加载失败时可以设置一个占位符
                        }
                    });


        }

        section_text.setMovementMethod(LocalLinkMovementMethod.getInstance());
        setLongClickForView(section_text, entity.getAttributed_child_section_text());
        setLongClickForView(section_note, entity.getAttributed_child_section_note());
        setLongClickForView(section_video, entity.getAttributed_child_section_video());
        // 为sectiontext设置点击监听，处理点击事件
        section_text.setOnClickListener(v -> {
            Boolean isClick = (Boolean) v.getTag();
            if (isClick != null && isClick) return;
            // EasyLog.print("条文点击: " + v.getTag() + ", 实体信息: " + entity);
            toggleVisibility(section_note, entity.getAttributed_child_section_note());
        });

        // 为sectionnote设置点击监听，处理点击事件
        section_note.setOnClickListener(v -> {
            if (v == null) return;

            Boolean isClick = (Boolean) v.getTag();
            if (isClick != null && isClick) return;

            int videoLength = 0;
            try {
                videoLength = entity.getAttributed_child_section_video().length();
            } catch (Exception e) {
                //e.printStackTrace();
                return;
            }
            boolean isVideoAvailable = videoLength > 0;

            if (section_note.getVisibility() == View.VISIBLE && section_video.getVisibility() == View.GONE && !isSectionvideo) {
                section_note.setVisibility(View.GONE);
                isSectionvideo = true;
                return;
            }
            if (isVideoAvailable) {
                isSectionvideo = false;
            }  else {
            section_note.setVisibility(View.GONE);
            isSectionvideo = true;
            return;
        }
            toggleVisibility(section_video, entity.getAttributed_child_section_video());
        });

        // 为sectionvideo设置点击监听，处理点击事件
        section_video.setOnClickListener(v -> {
            Boolean isClick = (Boolean) v.getTag();
            if (isClick != null && isClick) return;
            if (section_video.getVisibility() == View.VISIBLE ) {
                section_video.setVisibility(View.GONE);
                isSectionvideo= false;
                return;
            }
            toggleVisibility(section_video, entity.getAttributed_child_section_video());
        });

    }
    boolean isSectionvideo = false;
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

            TipsNetHelper.showListDialog(v.getContext(),AppConst.data_Type)
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
