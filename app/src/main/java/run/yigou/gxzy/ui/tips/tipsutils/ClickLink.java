/*
 * 项目名: AndroidProject
 * 类名: ClickLink.java
 * 包名: run.yigou.gxzy.ui.tips.tipsutils.ClickLink
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月12日 09:47:06
 * 上次修改时间: 2024年09月12日 09:44:38
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips.tipsutils;

import android.text.style.ClickableSpan;
import android.widget.TextView;

/* loaded from: classes.dex */
public interface ClickLink {
    void clickFangLink(TextView textView, ClickableSpan clickableSpan);

    void clickYaoLink(TextView textView, ClickableSpan clickableSpan);
}
