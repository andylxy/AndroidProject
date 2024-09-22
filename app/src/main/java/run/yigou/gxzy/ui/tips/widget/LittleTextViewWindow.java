package run.yigou.gxzy.ui.tips.widget;

import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import run.yigou.gxzy.R;
import run.yigou.gxzy.ui.activity.TipsFragmentActivity;
import run.yigou.gxzy.ui.tips.tipsutils.DataBeans.Fang;
import run.yigou.gxzy.ui.tips.tipsutils.DataBeans.Yao;
import run.yigou.gxzy.ui.tips.tipsutils.DataItem;
import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;
import run.yigou.gxzy.ui.tips.tipsutils.Singleton_Net_Data;
import run.yigou.gxzy.ui.tips.tipsutils.TipsNetHelper;
import run.yigou.gxzy.ui.tips.tipsutils.Tips_Single_Data;

/* loaded from: classes.dex */
public class LittleTextViewWindow extends LittleWindow {
    private SpannableStringBuilder attributedString;
    private ViewGroup mGroup;
    private Rect rect;
    private String tag = "littleWindow";
    private View view;
    private String yao;
    private Tips_Single_Data tipsSingleData;

    @Override // me.huanghai.searchController.LittleWindow
    public String getSearchString() {
        return this.yao;
    }

    public LittleTextViewWindow() {
        this.tipsSingleData = Tips_Single_Data.getInstance();
        ;
    }

    @Override // me.huanghai.searchController.LittleWindow
    public void show(FragmentManager fragmentManager) {
        super.show(fragmentManager);

        tipsSingleData.littleWindowStack.add(this);
    }

    @Override
    public void dismiss() {
        super.dismiss();
        tipsSingleData.littleWindowStack.remove(this);
    }

    public void setAttributedString(SpannableStringBuilder spannableStringBuilder) {
        this.attributedString = spannableStringBuilder;
    }

    public void setYao(String str) {
        String str2 = tipsSingleData.getYaoAliasDict().get(str);
        if (str2 != null) {
            str = str2;
        }
        this.yao = str;
    }

    public void setRect(Rect rect) {
        this.rect = rect;
    }

    public boolean isInYaoContext() {
        // 获取可变字符串
        SpannableStringBuilder spannableStringBuilder = this.attributedString;

        // 空指针检查
        if (spannableStringBuilder == null) {
            return false;
        }

        // 获取所有可点击的跨度
        ClickableSpan[] clickableSpanArr = spannableStringBuilder.getSpans(0, spannableStringBuilder.length(), ClickableSpan.class);

        // 如果存在可点击的跨度
        if (clickableSpanArr.length > 0) {
            ClickableSpan clickableSpan = clickableSpanArr[0]; // 取第一个可点击跨度
            int spanStart = spannableStringBuilder.getSpanStart(clickableSpan); // 获取跨度开始位置

            // 索引范围检查
            if (spanStart > 0) {
                String charSequence = spannableStringBuilder.subSequence(spanStart, spannableStringBuilder.getSpanEnd(clickableSpan)).toString(); // 获取跨度文本

                // 检查是否有对应的姚别名
                Map<String, String> yaoAliasDict = tipsSingleData.getYaoAliasDict();
                String alias = yaoAliasDict.get(charSequence);
                if (alias != null) {
                    charSequence = alias; // 使用姚别名替代原文本
                }

                // 判断是否在姚的上下文中
                if (tipsSingleData.getAllYao().contains(charSequence)
                        && spannableStringBuilder.toString().substring(spanStart - 1, spanStart).equals("、")) { // 确保前一个字符为“、”
                    return true; // 是姚上下文
                }
            }
        }
        return false; // 不是姚上下文
    }


//    protected SpannableStringBuilder getSpanString(final String str) {
//        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
//        SingletonData singletonData = SingletonData.getInstance();
//        Map<String, String> yaoAliasDict = singletonData.getYaoAliasDict();
//        String str2 = yaoAliasDict.get(str);
//        if (str2 != null) {
//            str = str2;
//        }
//        if (!onlyShowRelatedFang()) {
//            Iterator<HH2SectionData> it = singletonData.getYaoData().iterator();
//            while (it.hasNext()) {
//                for (DataItem dataItem : it.next().getData()) {
//                    String str3 = dataItem.getYaoList().get(0);
//                    String str4 = yaoAliasDict.get(str3);
//                    if (str4 != null) {
//                        str3 = str4;
//                    }
//                    if (str3.equals(str)) {
//                        spannableStringBuilder.append((CharSequence) dataItem.getAttributedText());
//                    }
//                }
//            }
//            if (spannableStringBuilder.length() == 0) {
//                spannableStringBuilder.append((CharSequence) Helper.renderText("$r{药物未找到资料}"));
//            }
//            spannableStringBuilder.append((CharSequence) "\n\n");
//        }
//        Iterator<HH2SectionData> it2 = SingletonData.getInstance().getFang().iterator();
//        int i = 0;
//        while (it2.hasNext()) {
//            HH2SectionData next = it2.next();
//            SpannableStringBuilder spannableStringBuilder2 = new SpannableStringBuilder();
//            List<DataItem> filter = Helper.filter(next.getData(),
//              new Helper.IFilter<DataItem>() { // from class: me.huanghai.searchController.LittleTextViewWindow.1
//                @Override // me.huanghai.searchController.Helper.IFilter
//                public boolean filter(DataItem dataItem2) {
//                    return ((Fang) dataItem2).hasYao(str);
//                }
//            });
//            Collections.sort(filter, new Comparator<DataItem>() { // from class: me.huanghai.searchController.LittleTextViewWindow.2
//                @Override // java.util.Comparator
//                public int compare(DataItem dataItem2, DataItem dataItem3) {
//                    return ((Fang) dataItem2).compare((Fang) dataItem3, str);
//                }
//            });
//            int i2 = 0;
//            for (DataItem dataItem2 : filter) {
//                Iterator<String> it3 = dataItem2.getYaoList().iterator();
//                while (true) {
//                    if (!it3.hasNext()) {
//                        break;
//                    }
//                    String next2 = it3.next();
//                    String str5 = yaoAliasDict.get(next2);
//                    if (str5 != null) {
//                        next2 = str5;
//                    }
//                    if (next2.equals(str)) {
//                        i2++;
//                        spannableStringBuilder2.append((CharSequence) Helper.renderText(((Fang) dataItem2).getFangNameLinkWithYaoWeight(str)));
//                        break;
//                    }
//                }
//            }
//            if (i > 0) {
//                spannableStringBuilder.append((CharSequence) "\n\n");
//            }
//            spannableStringBuilder.append((CharSequence) Helper.renderText(String.format("$m{%s}-$m{含“$v{%s}”凡%d方：}", next.getHeader(), str, Integer.valueOf(i2))));
//            if (i2 > 0) {
//                spannableStringBuilder.append((CharSequence) "\n");
//                spannableStringBuilder.append((CharSequence) spannableStringBuilder2);
//            }
//            i++;
//        }
//        return spannableStringBuilder;
//    }

    /**
     * 根据给定的药物名称获取SpannableStringBuilder对象，该对象包含了与药物相关的文本信息。
     * 这些信息包括药物自身的详细描述和包含该药物的方剂信息。
     *
     * @param str 药物名称，用于查询和生成相关信息。
     * @return SpannableStringBuilder 包含药物详细信息和相关方剂信息的SpannableStringBuilder对象。
     */
    protected SpannableStringBuilder getSpanString(String str) {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();

        Map<String, String> yaoAliasDict = tipsSingleData.getYaoAliasDict();

        // 获取药物的别名，若无则保留原名
        String alias = yaoAliasDict.get(str);
        str = alias != null ? alias : str;

        // 仅在不限制方剂显示时进行处理
        if (!onlyShowRelatedFang()) {
            // 遍历药物数据
            // 空值检查
            if (tipsSingleData == null || tipsSingleData.getYaoMap() == null) {
                throw new IllegalArgumentException("tipsSingleData or yaoMap is null");
            }
            Map<String, Yao> yaoMap = tipsSingleData.getYaoMap();
            Yao yaoData = yaoMap.get(str);
            if (yaoData == null) {
                // 处理 yaoData 为 null 的情况
                return spannableStringBuilder.append((CharSequence) TipsNetHelper.renderText("$r{药物未找到资料}"));

            }

            // 获取药物名称并查找别名
            String yaoName = yaoData.getName();
            String yaoAlias = yaoAliasDict.get(yaoName);
            yaoName = yaoAlias != null ? yaoAlias : yaoName;

            // 如果药物名称匹配，则添加其相关信息
            if (Objects.equals(yaoName, str)) {
                spannableStringBuilder.append((CharSequence) yaoData.getAttributedText());
            }


            // 如果没有找到相关药物，添加提示信息
            if (spannableStringBuilder.length() == 0) {
                spannableStringBuilder.append((CharSequence) TipsNetHelper.renderText("$r{药物未找到资料}"));
            }
            spannableStringBuilder.append("\n\n");
        }

        // 处理方剂数据
        int sectionCount = 0;
        for (HH2SectionData sectionData : tipsSingleData.getCurSingletonData().getFang()) {
            SpannableStringBuilder fangBuilder = new SpannableStringBuilder();

            // 筛选与药物相关的方剂
            String finalStr1 = str;
            // List<DataItem> filteredFang = (List<DataItem>) TipsNetHelper.filter(sectionData.getData(), dataItem -> ((Fang) dataItem).hasYao(finalStr1));

            // 假设 TipsNetHelper.filter 方法已知返回类型为 List<DataItem>
            List<DataItem> filteredFang = (List<DataItem>) TipsNetHelper.filter(
                    sectionData.getData() != null ? sectionData.getData() : Collections.emptyList(),
                    dataItem -> {
                        if (dataItem instanceof Fang) {
                            return ((Fang) dataItem).hasYao(finalStr1);
                        }
                        return false; // 或者抛出异常，取决于业务需求
                    }
            );


            String finalStr = str;
            // 对筛选结果进行排序
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                filteredFang.sort(new Comparator<DataItem>() {
                    @Override
                    public int compare(DataItem dataItem1, DataItem dataItem2) {
                        return ((Fang) dataItem1).compare((Fang) dataItem2, finalStr);
                    }
                });
            } else {
                Collections.sort(filteredFang, new Comparator<DataItem>() {
                    @Override
                    public int compare(DataItem dataItem1, DataItem dataItem2) {
                        return ((Fang) dataItem1).compare((Fang) dataItem2, finalStr);
                    }
                });
            }

            int matchedCount = 0;

            // 遍历筛选后的方剂
            for (DataItem dataItem : filteredFang) {
                for (String yaoName : dataItem.getYaoList()) {
                    // 查找药物的别名
                    String yaoAlias = yaoAliasDict.get(yaoName);
                    yaoName = yaoAlias != null ? yaoAlias : yaoName;

                    // 匹配方剂中的药物
                    if (yaoName.equals(str)) {
                        matchedCount++;
                        // 添加方剂名称及其相关药物信息
                        fangBuilder.append((CharSequence) TipsNetHelper.renderText(((Fang) dataItem).getFangNameLinkWithYaoWeight(str)));
                        break; // 匹配后退出内层循环
                    }
                }
            }

            // 添加方剂标题和匹配药物数量
            if (matchedCount > 0) {
                if (sectionCount > 0) {
                    spannableStringBuilder.append("\n\n"); // 分隔不同方剂
                }
                spannableStringBuilder.append((CharSequence) TipsNetHelper.renderText(
                        String.format("$m{%s}-$m{含“$v{%s}”凡%d方：}", sectionData.getHeader(), str, matchedCount)));
                spannableStringBuilder.append("\n").append(fangBuilder);
                sectionCount++;
            }
        }

        return spannableStringBuilder; // 返回最终构建的字符串
    }


    protected boolean onlyShowRelatedFang() {
        // 获取 SingletonData 实例和药物别名字典

        Map<String, String> yaoAliasDict = tipsSingleData.getYaoAliasDict();
        String str = this.yao; // 当前药物名称

        // 检查 littleWindowStack 是否有元素
        if (!tipsSingleData.littleWindowStack.isEmpty()) {
            // 获取栈顶元素的搜索字符串
            String searchString = tipsSingleData.littleWindowStack.get(tipsSingleData.littleWindowStack.size() - 1).getSearchString();

            // 查找搜索字符串的别名
            String str2 = yaoAliasDict.get(searchString);
            if (str2 != null) {
                searchString = str2; // 如果有别名，则使用别名替代
            }

            // 比较搜索字符串和当前药物名称，并检查上下文
            return searchString.equals(str) && isInYaoContext();
        }

        // 如果没有栈元素，检查当前显示的 Fragment 是否打开
       // ShowFragment showFragment = singletonData.curFragment;
       // return showFragment != null && showFragment.getIsContentOpen();
        return true;
    }


    @Override
    @Nullable
    /**
     * 创建视图
     *
     * @param layoutInflater 布局填充器
     * @param viewGroup 父容器
     * @param bundle 保存实例状态的Bundle
     * @return 返回创建的视图
     */
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        // 获取屏幕装饰视图作为父布局
        this.mGroup = (ViewGroup) getActivity().getWindow().getDecorView();
        // 获取屏幕高度和宽度
        int 屏幕高度 = this.mGroup.getHeight();
        int 屏幕宽度 = this.mGroup.getWidth();
        // 计算最小尺寸，用于确定箭头的大小
        int 最小尺寸 = Math.min(50, 屏幕宽度 / 18);

        // 创建布局参数
        FrameLayout.LayoutParams 箭头布局参数 = new FrameLayout.LayoutParams(最小尺寸, 最小尺寸);
        FrameLayout.LayoutParams 内容布局参数 = new FrameLayout.LayoutParams(-1, -2);

        // 计算目标位置的中心点坐标
        int 中心高度 = this.rect.top + (this.rect.height() / 2);
        int 中心宽度 = this.rect.left + (this.rect.width() / 2);

        // 确定箭头方向（UP为向上，DOWN为向下）
        int 箭头方向;
        if (中心高度 < 屏幕高度 / 2) {
            // 如果目标位置的中心高度在屏幕中上部，则箭头方向设置为向下
            this.view = layoutInflater.inflate(R.layout.show_yao, null);
            // 设置内容布局参数和箭头布局参数的边距
            内容布局参数.setMargins(最小尺寸, this.rect.top + this.rect.height() + 最小尺寸, 最小尺寸, 最小尺寸);
            int 一半最小尺寸 = 最小尺寸 / 2;
            箭头布局参数.setMargins(中心宽度 - 一半最小尺寸, this.rect.top + this.rect.height() + 4, (屏幕宽度 - 中心宽度) - 一半最小尺寸, (屏幕高度 - this.rect.top - this.rect.height() - 最小尺寸) - 4);
            箭头方向 = ArrowView.UP;
        } else {
            // 如果目标位置的中心高度在屏幕中下部，则箭头方向设置为向上
            this.view = layoutInflater.inflate(R.layout.show_yao_2, null);
            // 设置内容布局参数的Gravity为底部对齐
            内容布局参数.gravity = 80; // Gravity.BOTTOM
            // 获取可见区域的顶部位置
            Rect 可见区域 = new Rect();
            this.mGroup.getWindowVisibleDisplayFrame(可见区域);
            // 设置内容布局参数和箭头布局参数的边距
            内容布局参数.setMargins(最小尺寸, 可见区域.top + 8, 最小尺寸, (屏幕高度 - this.rect.top) + 最小尺寸);
            int 一半最小尺寸 = 最小尺寸 / 2;
            箭头布局参数.setMargins(中心宽度 - 一半最小尺寸, (this.rect.top - 最小尺寸) - 4, (屏幕宽度 - 中心宽度) - 一半最小尺寸, (屏幕高度 - this.rect.top) + 4);
            箭头方向 = ArrowView.DOWN;
        }

        // 设置关闭按钮的点击事件，点击时关闭当前视图
        ((Button) this.view.findViewById(R.id.maskbtnYao)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LittleTextViewWindow.this.dismiss();
            }
        });

        // 设置文本内容
        final TextView 文本标签 = this.view.findViewById(R.id.textview);
        // 设置文本内容为"未找到"，如果yao有值则设置为yao的内容
        文本标签.setText("未找到");
        final String 显示文本 = this.yao != null ? this.yao : "";
        文本标签.setText(getSpanString(显示文本));
        文本标签.setMovementMethod(LocalLinkMovementMethod.getInstance());

        // 设置箭头方向
        ArrowView 箭头视图 = this.view.findViewById(R.id.arrow);
        箭头视图.setDirection(箭头方向);
        箭头视图.setLayoutParams(箭头布局参数);

        // 设置左侧按钮点击事件，点击时将文本内容复制到剪贴板
        ((Button) this.view.findViewById(R.id.leftbtn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TipsNetHelper.copyToClipboard(getActivity(), 文本标签.getText().toString());
                Toast.makeText(getActivity(), "已复制到剪贴板", Toast.LENGTH_SHORT).show();
            }
        });

        // 设置右侧按钮点击事件，点击时启动MainActivity
        ((Button) this.view.findViewById(R.id.rightbtn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LittleTextViewWindow.this.getActivity(), TipsFragmentActivity.class);
                intent.putExtra("title", 显示文本);
                intent.putExtra("isFang", "false");
                intent.putExtra("yaoZheng", "true");
                LittleTextViewWindow.this.getActivity().startActivity(intent);
            }
        });

        // 设置内容布局参数并添加到父布局中
        ((LinearLayout) this.view.findViewById(R.id.wrapper)).setLayoutParams(内容布局参数);
        this.mGroup.addView(this.view);
        // 调用父类的onCreateView方法
        return super.onCreateView(layoutInflater, viewGroup, bundle);
    }


    @Override // android.app.Fragment
    public void onDestroyView() {
        this.mGroup.removeView(this.view);
        super.onDestroyView();
    }
}
