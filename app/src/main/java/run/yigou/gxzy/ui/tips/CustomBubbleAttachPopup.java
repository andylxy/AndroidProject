/*
 * 项目名: AndroidProject
 * 类名: CustomBubbleAttachPopup.java
 * 包名: run.yigou.gxzy.ui.tips.CustomBubbleAttachPopup
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月07日 08:18:15
 * 上次修改时间: 2024年09月07日 08:18:15
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.core.BubbleAttachPopupView;
import com.lxj.xpopup.util.XPopupUtils;

import run.yigou.gxzy.R;

public class CustomBubbleAttachPopup extends BubbleAttachPopupView {
    public CustomBubbleAttachPopup(@NonNull Context context) {
        super(context);
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.custom_bubble_attach_popup;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        //setBubbleBgColor(Color.BLUE);
        setBubbleShadowSize(XPopupUtils.dp2px(getContext(), 6));
        setBubbleShadowColor(Color.RED);
        setArrowWidth(XPopupUtils.dp2px(getContext(), 8));
        setArrowHeight(XPopupUtils.dp2px(getContext(), 20));
//                                .setBubbleRadius(100)
        setArrowRadius(XPopupUtils.dp2px(getContext(), 2));
        final TextView tv = findViewById(R.id.tv);
        Glide.with(getContext()).load("https://t7.baidu.com/it/u=963301259,1982396977&fm=193&f=GIF").into((ImageView) findViewById(R.id.image));
        tv.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
//                tv.setText(tv.getText() + "\n 啊哈哈哈啊哈");
//                tv.setText("\n 啊哈哈哈啊哈");
                //  dismiss();
                new XPopup.Builder(getContext())
                        //.maxHeight(800)
                        .isTouchThrough(true)
                        .isDestroyOnDismiss(true) //对于只使用一次的弹窗，推荐设置这个
                        .atView(v)
                        // .hasShadowBg(false) // 去掉半透明背景
                        .asCustom(new CustomBubbleAttachPopup(getContext()))
                        .show();
            }
        });
    }
}