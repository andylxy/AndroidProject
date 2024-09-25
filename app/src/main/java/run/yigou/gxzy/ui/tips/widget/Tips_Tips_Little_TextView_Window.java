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
import run.yigou.gxzy.manager.ReferenceManager;
import run.yigou.gxzy.ui.activity.TipsFragmentActivity;
import run.yigou.gxzy.ui.tips.tipsutils.DataBeans.Fang;
import run.yigou.gxzy.ui.tips.tipsutils.DataBeans.Yao;
import run.yigou.gxzy.ui.tips.tipsutils.DataItem;
import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;
import run.yigou.gxzy.ui.tips.tipsutils.TipsNetHelper;
import run.yigou.gxzy.ui.tips.tipsutils.Tips_Single_Data;

/* loaded from: classes.dex */
public class Tips_Tips_Little_TextView_Window extends Tips_Little_Window {
    private SpannableStringBuilder attributedString;
    private ViewGroup mGroup;
    private Rect rect;
    private String tag = "littleWindow";
    private View view;
    private String yao;
    private Tips_Single_Data tipsSingleData;

    @Override
    public String getSearchString() {
        return this.yao;
    }

    public Tips_Tips_Little_TextView_Window() {
        this.tipsSingleData = Tips_Single_Data.getInstance();

    }

    @Override
    public void show(FragmentManager fragmentManager) {
        super.show(fragmentManager);
        // 添加引用
        ReferenceManager.getInstance().addReference(REFERENCE_KEY, this);
        tipsSingleData.tipsLittleWindowStack.add(this);
    }

    @Override
    public void dismiss() {
        super.dismiss();
        tipsSingleData.tipsLittleWindowStack.remove(this);
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

                // 检查是否有对应的药别名
                Map<String, String> yaoAliasDict = tipsSingleData.getYaoAliasDict();
                String alias = yaoAliasDict.get(charSequence);
                if (alias != null) {
                    charSequence = alias; // 使用药别名替代原文本
                }

                // 判断是否在药的上下文中
                if (tipsSingleData.getAllYao().contains(charSequence)
                        && spannableStringBuilder.toString().substring(spanStart - 1, spanStart).equals("、")) { // 确保前一个字符为“、”
                    return true; // 是药上下文
                }
            }
        }
        return false; // 不是药上下文
    }

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
     //   if (!onlyShowRelatedFang()) {
        if (true) {
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
        if (!tipsSingleData.tipsLittleWindowStack.isEmpty()) {
            // 获取栈顶元素的搜索字符串
            String searchString = tipsSingleData.tipsLittleWindowStack.get(tipsSingleData.tipsLittleWindowStack.size() - 1).getSearchString();

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
        int screenHeight = this.mGroup.getHeight();
        int screenWidth = this.mGroup.getWidth();
        // 计算最小尺寸，用于确定箭头的大小
        int minSize = Math.min(50, screenWidth / 18);

        // 创建布局参数
        FrameLayout.LayoutParams arrowLayoutParams = new FrameLayout.LayoutParams(minSize, minSize);
        FrameLayout.LayoutParams contentLayoutParams = new FrameLayout.LayoutParams(-1, -2);

        // 计算目标位置的中心点坐标
        int centerHeight = this.rect.top + (this.rect.height() / 2);
        int centerWidth = this.rect.left + (this.rect.width() / 2);

        // 确定箭头方向（UP为向上，DOWN为向下）
        int arrowDirection;
        if (centerHeight < screenHeight / 2) {
            // 如果目标位置的中心高度在屏幕中上部，则箭头方向设置为向下
            this.view = layoutInflater.inflate(R.layout.show_yao, null);
            // 设置内容布局参数和箭头布局参数的边距
            contentLayoutParams.setMargins(minSize, this.rect.top + this.rect.height() + minSize, minSize, minSize);
            int halfMinSize = minSize / 2;
            arrowLayoutParams.setMargins(centerWidth - halfMinSize, this.rect.top + this.rect.height() + 4,
                    (screenWidth - centerWidth) - halfMinSize,
                    (screenHeight - this.rect.top - this.rect.height() - minSize) - 4);
            arrowDirection = Tips_ArrowView.UP;
        } else {
            // 如果目标位置的中心高度在屏幕中下部，则箭头方向设置为向上
            this.view = layoutInflater.inflate(R.layout.show_yao_2, null);
            // 设置内容布局参数的Gravity为底部对齐
            contentLayoutParams.gravity = 80; // Gravity.BOTTOM
            // 获取可见区域的顶部位置
            Rect visibleRect = new Rect();
            this.mGroup.getWindowVisibleDisplayFrame(visibleRect);
            // 设置内容布局参数和箭头布局参数的边距
            contentLayoutParams.setMargins(minSize, visibleRect.top + 8, minSize, (screenHeight - this.rect.top) + minSize);
            int halfMinSize = minSize / 2;
            arrowLayoutParams.setMargins(centerWidth - halfMinSize, (this.rect.top - minSize) - 4,
                    (screenWidth - centerWidth) - halfMinSize,
                    (screenHeight - this.rect.top) + 4);
            arrowDirection = Tips_ArrowView.DOWN;
        }

        // 设置关闭按钮的点击事件，点击时关闭当前视图
        ((Button) this.view.findViewById(R.id.maskbtnYao)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Tips_Tips_Little_TextView_Window.this.dismiss();
            }
        });

        // 设置文本内容
        final TextView textLabel = this.view.findViewById(R.id.textview);
        // 设置文本内容为"未找到"，如果yao有值则设置为yao的内容
        textLabel.setText("未找到");
        final String displayText = this.yao != null ? this.yao : "";
        textLabel.setText(getSpanString(displayText));
        textLabel.setMovementMethod(LocalLinkMovementMethod.getInstance());

        // 设置箭头方向
        Tips_ArrowView arrowView = this.view.findViewById(R.id.arrow);
        arrowView.setDirection(arrowDirection);
        arrowView.setLayoutParams(arrowLayoutParams);

        // 设置左侧按钮点击事件，点击时将文本内容复制到剪贴板
        ((Button) this.view.findViewById(R.id.leftbtn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TipsNetHelper.copyToClipboard(getActivity(), textLabel.getText().toString());
                Toast.makeText(getActivity(), "已复制到剪贴板", Toast.LENGTH_SHORT).show();
            }
        });

        // 设置右侧按钮点击事件，点击时启动MainActivity
        ((Button) this.view.findViewById(R.id.rightbtn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Tips_Tips_Little_TextView_Window.this.getActivity(), TipsFragmentActivity.class);
                intent.putExtra("title", displayText);
                intent.putExtra("isFang", "false");
                intent.putExtra("yaoZheng", "true");
                Tips_Tips_Little_TextView_Window.this.getActivity().startActivity(intent);
            }
        });

        // 设置内容布局参数并添加到父布局中
        ((LinearLayout) this.view.findViewById(R.id.wrapper)).setLayoutParams(contentLayoutParams);
        this.mGroup.addView(this.view);
        // 调用父类的onCreateView方法
        return super.onCreateView(layoutInflater, viewGroup, bundle);
    }

//    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
//        // 获取屏幕装饰视图作为父布局
//        this.mGroup = (ViewGroup) getActivity().getWindow().getDecorView();
//        // 获取屏幕高度和宽度
//        int 屏幕高度 = this.mGroup.getHeight();
//        int 屏幕宽度 = this.mGroup.getWidth();
//        // 计算最小尺寸，用于确定箭头的大小
//        int 最小尺寸 = Math.min(50, 屏幕宽度 / 18);
//
//        // 创建布局参数
//        FrameLayout.LayoutParams 箭头布局参数 = new FrameLayout.LayoutParams(最小尺寸, 最小尺寸);
//        FrameLayout.LayoutParams 内容布局参数 = new FrameLayout.LayoutParams(-1, -2);
//
//        // 计算目标位置的中心点坐标
//        int 中心高度 = this.rect.top + (this.rect.height() / 2);
//        int 中心宽度 = this.rect.left + (this.rect.width() / 2);
//
//        // 确定箭头方向（UP为向上，DOWN为向下）
//        int 箭头方向;
//        if (中心高度 < 屏幕高度 / 2) {
//            // 如果目标位置的中心高度在屏幕中上部，则箭头方向设置为向下
//            this.view = layoutInflater.inflate(R.layout.show_yao, null);
//            // 设置内容布局参数和箭头布局参数的边距
//            内容布局参数.setMargins(最小尺寸, this.rect.top + this.rect.height() + 最小尺寸, 最小尺寸, 最小尺寸);
//            int 一半最小尺寸 = 最小尺寸 / 2;
//            箭头布局参数.setMargins(中心宽度 - 一半最小尺寸, this.rect.top + this.rect.height() + 4, (屏幕宽度 - 中心宽度) - 一半最小尺寸, (屏幕高度 - this.rect.top - this.rect.height() - 最小尺寸) - 4);
//            箭头方向 = Tips_ArrowView.UP;
//        } else {
//            // 如果目标位置的中心高度在屏幕中下部，则箭头方向设置为向上
//            this.view = layoutInflater.inflate(R.layout.show_yao_2, null);
//            // 设置内容布局参数的Gravity为底部对齐
//            内容布局参数.gravity = 80; // Gravity.BOTTOM
//            // 获取可见区域的顶部位置
//            Rect 可见区域 = new Rect();
//            this.mGroup.getWindowVisibleDisplayFrame(可见区域);
//            // 设置内容布局参数和箭头布局参数的边距
//            内容布局参数.setMargins(最小尺寸, 可见区域.top + 8, 最小尺寸, (屏幕高度 - this.rect.top) + 最小尺寸);
//            int 一半最小尺寸 = 最小尺寸 / 2;
//            箭头布局参数.setMargins(中心宽度 - 一半最小尺寸, (this.rect.top - 最小尺寸) - 4, (屏幕宽度 - 中心宽度) - 一半最小尺寸, (屏幕高度 - this.rect.top) + 4);
//            箭头方向 = Tips_ArrowView.DOWN;
//        }
//
//        // 设置关闭按钮的点击事件，点击时关闭当前视图
//        ((Button) this.view.findViewById(R.id.maskbtnYao)).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Tips_Tips_Little_TextView_Window.this.dismiss();
//            }
//        });
//
//        // 设置文本内容
//        final TextView 文本标签 = this.view.findViewById(R.id.textview);
//        // 设置文本内容为"未找到"，如果yao有值则设置为yao的内容
//        文本标签.setText("未找到");
//        final String 显示文本 = this.yao != null ? this.yao : "";
//        文本标签.setText(getSpanString(显示文本));
//        文本标签.setMovementMethod(LocalLinkMovementMethod.getInstance());
//
//        // 设置箭头方向
//        Tips_ArrowView 箭头视图 = this.view.findViewById(R.id.arrow);
//        箭头视图.setDirection(箭头方向);
//        箭头视图.setLayoutParams(箭头布局参数);
//
//        // 设置左侧按钮点击事件，点击时将文本内容复制到剪贴板
//        ((Button) this.view.findViewById(R.id.leftbtn)).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                TipsNetHelper.copyToClipboard(getActivity(), 文本标签.getText().toString());
//                Toast.makeText(getActivity(), "已复制到剪贴板", Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        // 设置右侧按钮点击事件，点击时启动MainActivity
//        ((Button) this.view.findViewById(R.id.rightbtn)).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent(Tips_Tips_Little_TextView_Window.this.getActivity(), TipsFragmentActivity.class);
//                intent.putExtra("title", 显示文本);
//                intent.putExtra("isFang", "false");
//                intent.putExtra("yaoZheng", "true");
//                Tips_Tips_Little_TextView_Window.this.getActivity().startActivity(intent);
//            }
//        });
//
//        // 设置内容布局参数并添加到父布局中
//        ((LinearLayout) this.view.findViewById(R.id.wrapper)).setLayoutParams(内容布局参数);
//        this.mGroup.addView(this.view);
//        // 调用父类的onCreateView方法
//        return super.onCreateView(layoutInflater, viewGroup, bundle);
//    }


    @Override
    public void onDestroyView() {
        this.mGroup.removeView(this.view);
        super.onDestroyView();
    }

    private static final String REFERENCE_KEY = "Tips_Tips_Little_TextView_Window";

    @Override
    public void onDestroy() {
        super.onDestroy();
       // dismiss();
        // 清除引用
        ReferenceManager.getInstance().removeReference(REFERENCE_KEY);
    }
}
