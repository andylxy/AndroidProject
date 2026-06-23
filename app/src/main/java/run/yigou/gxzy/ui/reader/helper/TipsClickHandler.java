/*
 * 项目名: AndroidProject
 * 类名: TipsClickHandler.java
 * 包名: run.yigou.gxzy.ui.reader.helper
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月12日 09:47:06
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.reader.helper;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.util.Pair;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import run.yigou.gxzy.log.EasyLog;
import run.yigou.gxzy.text.ClickLink;
import run.yigou.gxzy.text.TipsTextRenderer;
import run.yigou.gxzy.ui.reader.entity.GroupData;
import run.yigou.gxzy.ui.reader.entity.ItemData;
import run.yigou.gxzy.ui.reader.search.SearchDataAdapter;
import run.yigou.gxzy.ui.reader.widget.TipsLittleMingCiViewWindow;
import run.yigou.gxzy.ui.reader.widget.TipsLittleTableViewWindow;
import run.yigou.gxzy.tips.widget.ITipsWindowHost;
import run.yigou.gxzy.ui.reader.TipsFragmentActivity;

import java.util.List;

/**
 * Tips 模块点击事件处理器，负责处理文本中药物、方剂、名词链接的点击交互。
 *
 * <p>从 {@link TipsNetHelper} 中提取，消除三个 click 回调方法（clickYaoLink / clickFangLink /
 * clickMingCiLink）之间约 90% 的重复代码，统一为模板方法 {@link #handleClick(TextView, ClickableSpan, ClickType)}。
 *
 * <p>使用方式：
 * <pre>{@code
 *   SpannableStringBuilder ssb = TipsTextRenderer.renderText(text, TipsClickHandler.DEFAULT_CLICK_LINK);
 * }</pre>
 */
public class TipsClickHandler {

    /** 点击链接类型枚举，用于区分药物、方剂、名词三种链接 */
    private enum ClickType {
        /** 药物链接 */
        YAO,
        /** 方剂链接 */
        FANG,
        /** 名词链接 */
        MING_CI
    }

    /**
     * 默认的 ClickLink 实现，所有点击回调统一委托到 {@link #handleClick} 模板方法。
     *
     * <p>该实例是线程安全的单例，可在多处复用。
     */
    public static final ClickLink DEFAULT_CLICK_LINK = new ClickLink() {

        @Override
        public void onClickLink(int linkType, TextView textView, android.text.style.ClickableSpan span) {
            // 使用配置中心的 linkType 动态路由
            handleClickByLinkType(textView, span, linkType);
        }

        @Override
        public void clickYaoLink(TextView textView, ClickableSpan clickableSpan) {
            handleClick(textView, clickableSpan, ClickType.YAO);
        }

        @Override
        public void clickFangLink(TextView textView, ClickableSpan clickableSpan) {
            handleClick(textView, clickableSpan, ClickType.FANG);
        }

        @Override
        public void clickMingCiLink(TextView textView, ClickableSpan clickableSpan) {
            handleClick(textView, clickableSpan, ClickType.MING_CI);
        }
    };

    /**
     * 使用默认 ClickLink 渲染文本为富文本 SpannableStringBuilder。
     *
     * <p>等价于 {@code TipsTextRenderer.renderText(str, DEFAULT_CLICK_LINK)}，
     * 提供便捷的静态入口，避免调用方直接依赖 {@link TipsTextRenderer}。
     *
     * @param str 原始文本字符串，支持 {@code $x{...}} 等标记语法
     * @return 带有 ClickableSpan 的 SpannableStringBuilder
     */
    public static SpannableStringBuilder renderText(String str) {
        return TipsTextRenderer.renderText(str, DEFAULT_CLICK_LINK);
    }

    /**
     * 根据配置中心的 linkType 动态路由到对应的处理方法
     * 
     * @param textView  被点击的 TextView
     * @param span      被点击的 ClickableSpan
     * @param linkType  链接类型（由 StyleConfig.linkType 定义）
     */
    private static void handleClickByLinkType(TextView textView, ClickableSpan span, int linkType) {
        switch (linkType) {
            case 1:  // 药物
                handleClick(textView, span, ClickType.YAO);
                break;
            case 2:  // 方剂
                handleClick(textView, span, ClickType.FANG);
                break;
            case 3:  // 名词
                handleClick(textView, span, ClickType.MING_CI);
                break;
            default:
                EasyLog.print("❌ 未知的linkType: " + linkType);
                break;
        }
    }

    /**
     * 点击处理模板方法，统一处理药物/方剂/名词三种链接的点击交互流程。
     *
     * <p>处理流程：
     * <ol>
     *   <li>从 TextView 选中区域提取关键词</li>
     *   <li>校验 BookRepository 上下文是否已设置</li>
     *   <li>根据 ClickType 选择对应的搜索方法和弹窗类型</li>
     *   <li>计算点击位置矩形区域</li>
     *   <li>创建并显示对应的小窗（TipsLittleTableViewWindow 或 TipsLittleMingCiViewWindow）</li>
     * </ol>
     *
     * @param textView       被点击的 TextView
     * @param clickableSpan  被点击的 ClickableSpan
     * @param clickType      点击链接类型（药物/方剂/名词）
     */
    static void handleClick(TextView textView, ClickableSpan clickableSpan, ClickType clickType) {
        // 1. 提取关键词：从 TextView 选中区域获取文本
        String keyword = textView.getText()
                .subSequence(textView.getSelectionStart(), textView.getSelectionEnd())
                .toString();

        // 2. 校验 BookRepository 上下文
        if (TipsNetHelper.getBookRepository() == null || TipsNetHelper.getCurrentBookId() == -1) {
            EasyLog.print("❌ BookRepository未设置，无法搜索");
            return;
        }

        // 3. 根据点击类型执行对应的搜索
        SearchDataAdapter adapter = new SearchDataAdapter(
                TipsNetHelper.getBookRepository(), TipsNetHelper.getCurrentBookId());
        Pair<List<GroupData>, List<List<ItemData>>> data;
        switch (clickType) {
            case YAO:
                data = adapter.searchYaoContent(keyword.trim());
                break;
            case FANG:
                data = adapter.searchFangContent(keyword.trim());
                break;
            case MING_CI:
                data = adapter.searchMingCiContent(keyword.trim());
                break;
            default:
                EasyLog.print("❌ 未知的ClickType: " + clickType);
                return;
        }

        // 4. 计算点击位置矩形区域
        Rect textRect = TipsUIHelper.getTextRect(clickableSpan, textView);

        // 5. 校验 Context 并显示弹窗
        Context context = textView.getContext();
        if (!(context instanceof AppCompatActivity)) {
            EasyLog.print("❌ Context不是AppCompatActivity!");
            return;
        }
        AppCompatActivity activity = (AppCompatActivity) context;

        // 6. 根据类型创建对应的窗口并显示
        switch (clickType) {
            case YAO:
            case FANG: {
                TipsLittleTableViewWindow window = new TipsLittleTableViewWindow();
                window.setData(context, data);
                window.setFang(keyword);
                window.setRect(textRect);
                window.setHost(createWindowHost(activity));
                window.show(activity.getSupportFragmentManager());
                break;
            }
            case MING_CI: {
                TipsLittleMingCiViewWindow window = new TipsLittleMingCiViewWindow();
                window.setData(context, data);
                window.setRect(textRect);
                window.setHost(createWindowHost(activity));
                window.show(activity.getSupportFragmentManager());
                break;
            }
            default:
                break;
        }
    }

    /**
     * 创建统一的 ITipsWindowHost 实现，供所有弹窗类型复用。
     *
     * <p>三个原始 click 方法中此匿名类实现完全相同，提取后消除重复。
     *
     * @param activity 宿主 AppCompatActivity
     * @return ITipsWindowHost 实例
     */
    private static ITipsWindowHost createWindowHost(final AppCompatActivity activity) {
        return new ITipsWindowHost() {
            @Override
            public int getWrapperViewId() {
                return run.yigou.gxzy.R.id.wrapper;
            }

            @Override
            public void navigateToDetail(Intent intent) {
                intent.setClass(activity, TipsFragmentActivity.class);
                activity.startActivity(intent);
            }
        };
    }
}
