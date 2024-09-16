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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.lxj.xpopup.core.BubbleAttachPopupView;
import com.lxj.xpopup.util.XPopupUtils;

import run.yigou.gxzy.R;
import run.yigou.gxzy.ui.tips.widget.LocalLinkMovementMethod;
import run.yigou.gxzy.ui.tips.tipsutils.DataBeans.ShowFanYao;

@SuppressLint("ViewConstructor")
public class TipsWindow_Yao_BubbleAttachPopup extends BubbleAttachPopupView {
    String mYao ;//charSequence
    public TipsWindow_Yao_BubbleAttachPopup(@NonNull Context context, String charSequence) {
        super(context);
        mYao = charSequence;
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.custom_bubble_attach_popup;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        //setBubbleBgColor(Color.BLUE);
        setBubbleShadowSize(XPopupUtils.dp2px(getContext(), 2));
        setBubbleShadowColor(Color.RED);
        setArrowWidth(XPopupUtils.dp2px(getContext(), 5f));
        setArrowHeight(XPopupUtils.dp2px(getContext(), 10f));
//                                .setBubbleRadius(100)
        setArrowRadius(XPopupUtils.dp2px(getContext(), 2f));
        final TextView tv = findViewById(R.id.tv);
        ShowFanYao showFanYao =  new ShowFanYao();
        tv.setMovementMethod(LocalLinkMovementMethod.getInstance());
        tv.setText(showFanYao.getShowYaoSpanString(mYao));
    }

//    @Override
//    protected int getPopupHeight() {
//        return (int) (XPopupUtils.getScreenHeight(getContext())*.85f);
//    }


//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//
//        Log.d("CustomBubbleAttachPopup", "onTouchEvent: X: "+event.getX()+" Y: "+event.getY());
//        return super.onTouchEvent(event);
//    }
}