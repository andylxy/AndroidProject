package run.yigou.gxzy.ui.tips.widget;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.donkingliang.groupedadapter.adapter.GroupedRecyclerViewAdapter;
import com.donkingliang.groupedadapter.holder.BaseViewHolder;

import java.util.ArrayList;
import java.util.Map;

import run.yigou.gxzy.R;
import run.yigou.gxzy.ui.activity.TipsFragmentActivity;
import run.yigou.gxzy.ui.tips.adapter.NoFooterAdapter;
import run.yigou.gxzy.ui.tips.entity.GroupEntity;
import run.yigou.gxzy.ui.tips.entity.GroupModel;
import run.yigou.gxzy.ui.tips.tipsutils.DataBeans.Show_Fan_Yao_MingCi;
import run.yigou.gxzy.ui.tips.tipsutils.TipsNetHelper;
import run.yigou.gxzy.ui.tips.tipsutils.Tips_Single_Data;

/* loaded from: classes.dex */
public class LittleTableViewWindow extends LittleWindow {
    private SpannableStringBuilder attributedString;
    private String fang;
    private ViewGroup mGroup;
    private Rect rect;
    private String tag = "littleWindow";
    private View view;
    private Tips_Single_Data tips_single_data = Tips_Single_Data.getInstance();

    @Override // me.huanghai.searchController.LittleWindow
    public String getSearchString() {
        return this.fang;
    }

    @Override // me.huanghai.searchController.LittleWindow
    public void show(FragmentManager fragmentManager) {
        super.show(fragmentManager);
        tips_single_data.littleWindowStack.add(this);
    }

    @Override // me.huanghai.searchController.LittleWindow
    public void dismiss() {
        super.dismiss();
        tips_single_data.littleWindowStack.remove(this);
    }

    public void setAttributedString(SpannableStringBuilder spannableStringBuilder) {
        this.attributedString = spannableStringBuilder;
    }

    public void setFang(String str) {
        String str2 = tips_single_data.getFangAliasDict().get(str);
        if (str2 != null) {
            str = str2;
        }
        this.fang = str;
    }

    public void setRect(Rect rect) {
        this.rect = rect;
    }

    public boolean isInFangContext() {
        SpannableStringBuilder spannableStringBuilder = this.attributedString; // 获取可变字符串
        ClickableSpan[] clickableSpanArr = (ClickableSpan[]) spannableStringBuilder.getSpans(0, spannableStringBuilder.length(), ClickableSpan.class); // 获取所有可点击的跨度
        Map<String, String> fangAliasDict = tips_single_data.getFangAliasDict(); // 获取方别名字典

        // 如果存在可点击的跨度
        if (clickableSpanArr.length > 0) {
            ClickableSpan clickableSpan = clickableSpanArr[0]; // 取第一个可点击跨度
            int spanStart = spannableStringBuilder.getSpanStart(clickableSpan); // 获取跨度开始位置
            String charSequence = spannableStringBuilder.subSequence(spanStart, spannableStringBuilder.getSpanEnd(clickableSpan)).toString(); // 获取跨度文本

            // 检查是否有对应的方别名
            String str = fangAliasDict.get(charSequence);
            if (str != null) {
                charSequence = str; // 使用方别名替代原文本
            }

            // 判断是否在方的上下文中
            if (tips_single_data.getCurSingletonData().getAllFang().contains(charSequence)
                    && spanStart > 0
                    && spannableStringBuilder.toString().substring(spanStart - 1, spanStart).equals("、") // 确保前一个字符为“、”
                    && spannableStringBuilder.charAt(0) != '*') { // 确保第一个字符不是 '*'
                return true; // 是方上下文
            }
        }
        return false; // 不是方上下文
    }


    protected boolean onlyShowRelatedContent() {

        Map<String, String> fangAliasDict = tips_single_data.getFangAliasDict(); // 获取方别名字典
        String str = this.fang; // 当前的方

        // 如果小窗口堆栈中有内容
        if (!tips_single_data.littleWindowStack.isEmpty()) {
            // 获取小窗口栈顶的搜索字符串
            String searchString = tips_single_data.littleWindowStack.get(tips_single_data.littleWindowStack.size() - 1).getSearchString();

            // 检查是否有对应的方别名
            String str2 = fangAliasDict.get(searchString);
            if (str2 != null) {
                searchString = str2; // 使用方别名替代原搜索字符串
            }

            // 判断搜索字符串是否与当前方相同，并且是否在方的上下文中
            return searchString.equals(str) && isInFangContext();
        }
        // todo 待完善
        // 获取当前显示的片段
        // ShowFragment showFragment = singletonData.curFragment;
        // 如果当前片段不为空且内容已打开，则返回 true
        // return showFragment != null && showFragment.getIsContentOpen();
        return true;
    }


    //    public View onCreateVieworg(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
//        // 获取当前窗口的根视图
//        Activity activity = getActivity();
//        if (activity == null) {
//            throw new IllegalStateException("Activity cannot be null.");
//        }
//        this.mGroup = (ViewGroup) activity.getWindow().getDecorView();
//        // 初始化密度、宽度和高度
//        float density = this.mGroup.getResources().getDisplayMetrics().density;
//        int height = this.mGroup.getHeight();
//        int width = this.mGroup.getWidth();
//        int min = Math.min(50, width / 18);
//        // 设置FrameLayout的布局参数
//        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(min, min);
//        FrameLayout.LayoutParams layoutParams2 = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
//        // 初始化矩形区域
//        if (this.rect == null) {
//            this.rect = new Rect();
//        }
//        // 计算矩形区域的中心点
//        int height2 = this.rect.top + (this.rect.height() / 2);
//        int width2 = this.rect.left + (this.rect.width() / 2);
//        int i2 = width / 2;
//        int i3 = height / 2;
//        int i4 = ArrowView.UP;
//        // 根据矩形区域的位置决定布局方向
//        if (height2 < i3) {
//            // 显示向上的布局
//            this.view = layoutInflater.inflate(R.layout.show_fang, (ViewGroup) null);
//            layoutParams2.setMargins(min, this.rect.top + this.rect.height() + min, min, min);
//            int i5 = min / 2;
//            layoutParams.setMargins(width2 - i5, this.rect.top + this.rect.height(), (width - width2) - i5, (height - this.rect.top - this.rect.height()) - min);
//        } else {
//            // 显示向下的布局
//            this.view = layoutInflater.inflate(R.layout.show_fang_2, (ViewGroup) null);
//            layoutParams2.gravity = Gravity.BOTTOM;
//            Rect rect = new Rect();
//            this.mGroup.getWindowVisibleDisplayFrame(rect);
//            layoutParams2.setMargins(min, rect.top + 8, min, (height - this.rect.top) + min);
//            int i6 = min / 2;
//            layoutParams.setMargins(width2 - i6, (this.rect.top - min) - 1, (width - width2) - i6, (height - this.rect.top) + 1);
//        }
//        // 处理关闭按钮点击事件
//        Button closeButton = this.view.findViewById(R.id.maskbtn);
//        closeButton.setOnClickListener(v -> {
//            LittleTableViewWindow.this.dismiss();
//            SingletonData.getInstance().popShowFang();
//        });
//        // 初始化并配置ATableView
//        ATableView aTableView = this.view.findViewById(R.id.showfang);
//        aTableView.init(ATableView.ATableViewStyle.Plain);
//        final String str = this.fang != null ? this.fang : "";
//        final ShowFang showFang = new ShowFang(str, onlyShowRelatedContent());
//        SingletonData.getInstance().pushShowFang(showFang);
//        aTableView.setDataSource(showFang.getDataSource());
//        aTableView.setDelegate(showFang.getDelegate());
//        aTableView.enableHeaderView(true);
//        // 设置箭头方向
//        ArrowView arrowView = this.view.findViewById(R.id.arrow);
//        arrowView.setDirection(height2 < i3 ? ArrowView.UP : ArrowView.DOWN);
//        arrowView.setLayoutParams(layoutParams);
//        // 处理左侧按钮点击事件
//        Button leftButton = this.view.findViewById(R.id.leftbtn);
//        leftButton.setOnClickListener(v -> {
//            showFang.putCopyStringsToClipboard();
//            Toast.makeText(SingletonData.getInstance().curActivity, "已复制到剪贴板", Toast.LENGTH_SHORT).show();
//        });
//        // 处理右侧按钮点击事件
//        Button rightButton = this.view.findViewById(R.id.rightbtn);
//        rightButton.setOnClickListener(v -> {
//            Intent intent = new Intent(activity, MainActivity.class);
//            intent.putExtra("title", str);
//            intent.putExtra("isFang", "false");
//            activity.startActivity(intent);
//        });
//        // 配置Wrapper布局参数并添加视图
//        LinearLayout wrapper = this.view.findViewById(R.id.wrapper);
//        wrapper.setLayoutParams(layoutParams2);
//        this.mGroup.addView(this.view);
//        return super.onCreateView(layoutInflater, viewGroup, bundle);
//    }
    private NoFooterAdapter adapter;

    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        // 获取当前活动，确保不为null
        Activity activity = getActivity();
        if (activity == null) {
            throw new IllegalStateException("Activity cannot be null."); // 如果活动为null，则抛出异常
        }

        // 获取窗口的根视图和屏幕信息
        this.mGroup = (ViewGroup) activity.getWindow().getDecorView(); // 获取窗口的根视图
        DisplayMetrics metrics = this.mGroup.getResources().getDisplayMetrics(); // 获取显示信息
        float density = metrics.density; // 获取屏幕密度
        int height = this.mGroup.getHeight(); // 获取视图高度
        int width = this.mGroup.getWidth(); // 获取视图宽度

        // 计算最小值，用于视图的大小
        int minSize = Math.min(50, width / 18); // 计算最小尺寸

        // 创建布局参数
        FrameLayout.LayoutParams smallLayoutParams = new FrameLayout.LayoutParams(minSize, minSize); // 小尺寸布局参数
        FrameLayout.LayoutParams largeLayoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT // 大尺寸布局参数
        );

        // 初始化矩形区域
        if (this.rect == null) {
            this.rect = new Rect(); // 如果矩形为空，则初始化
        }

        // 计算矩形中心点
        int centerY = this.rect.top + (this.rect.height() / 2); // 计算Y轴中心
        int centerX = this.rect.left + (this.rect.width() / 2); // 计算X轴中心
        int midHeight = height / 2; // 计算屏幕中间高度

        // 根据矩形区域位置决定布局方向
        if (centerY < midHeight) {
            // 向上布局
            this.view = layoutInflater.inflate(R.layout.show_fang, null); // 加载向上布局视图
            largeLayoutParams.setMargins(minSize, this.rect.top + this.rect.height() + minSize, minSize, minSize); // 设置大布局参数的边距
            smallLayoutParams.setMargins(centerX - (minSize / 2), this.rect.top + this.rect.height(),
                    (width - centerX) - (minSize / 2),
                    (height - this.rect.top - this.rect.height()) - minSize); // 设置小布局参数的边距
        } else {
            // 向下布局
            this.view = layoutInflater.inflate(R.layout.show_fang_2, null); // 加载向下布局视图
            largeLayoutParams.gravity = Gravity.BOTTOM; // 设置重力为底部
            Rect visibleRect = new Rect(); // 初始化可见矩形
            this.mGroup.getWindowVisibleDisplayFrame(visibleRect); // 获取可见区域
            largeLayoutParams.setMargins(minSize, visibleRect.top + 8, minSize, (height - this.rect.top) + minSize); // 设置大布局边距
            smallLayoutParams.setMargins(centerX - (minSize / 2), (this.rect.top - minSize) - 1,
                    (width - centerX) - (minSize / 2), (height - this.rect.top) + 1); // 设置小布局边距
        }

        // 处理关闭按钮点击事件
        Button closeButton = this.view.findViewById(R.id.maskbtn); // 获取关闭按钮
        closeButton.setOnClickListener(v -> {
            dismiss(); // 关闭视图
            // todo: 待完善
            //SingletonData.getInstance().popShowFang(); // 更新单例数据
        });

//        // 初始化并配置ATableView
//        ATableView aTableView = this.view.findViewById(R.id.showfang); // 获取ATableView
//        aTableView.init(ATableView.ATableViewStyle.Plain); // 初始化表格样式
//
//        // 获取内容并更新单例数据
//        String content = (this.fang != null) ? this.fang : ""; // 获取内容
//        ShowFang showFang = new ShowFang(content, onlyShowRelatedContent()); // 创建ShowFang实例
//        SingletonData.getInstance().pushShowFang(showFang); // 更新单例数据
//        aTableView.setDataSource(showFang.getDataSource()); // 设置数据源
//        aTableView.setDelegate(showFang.getDelegate()); // 设置委托
//        aTableView.enableHeaderView(true); // 启用表头视图

        rvList = this.view.findViewById(R.id.include_tips_windows_sticky_list).findViewById(R.id.sticky_rv_list);
        //stickyLayout = (StickyHeaderLayout) findViewById(R.id.sticky_layout);

        rvList.setLayoutManager(new LinearLayoutManager(getActivity()));
        //加载药方和药物检查实例
        // if (fanYao == null)

        //检索所有的相关药方
        //if (adapter == null){
       // adapter = new NoFooterAdapter(getActivity());
        //}else {
        //   adapter.clear();
        //   adapter. setGroups(GroupModel.getGroups(fanYao.showFang(fanyao_name)));
        // }

        adapter.setOnHeaderClickListener(new GroupedRecyclerViewAdapter.OnHeaderClickListener() {
            @Override
            public void onHeaderClick(GroupedRecyclerViewAdapter adapter, BaseViewHolder holder,
                                      int groupPosition) {
//                Toast.makeText(mContext, "组头：groupPosition = " + groupPosition,
//                        Toast.LENGTH_LONG).show();
//                Log.e("eee", adapter.toString() + "  " + holder.toString());
            }
        });

        adapter.setOnChildClickListener(new GroupedRecyclerViewAdapter.OnChildClickListener() {
            @Override
            public void onChildClick(GroupedRecyclerViewAdapter adapter, BaseViewHolder holder,
                                     int groupPosition, int childPosition) {
//                Toast.makeText(mContext, "子项：groupPosition = " + groupPosition
//                                + ", childPosition = " + childPosition,
//                        Toast.LENGTH_LONG).show();
            }
        });
        rvList.setAdapter(adapter);


        // 设置箭头方向
        ArrowView arrowView = this.view.findViewById(R.id.arrow); // 获取箭头视图
        arrowView.setDirection(centerY < midHeight ? ArrowView.UP : ArrowView.DOWN); // 设置箭头方向
        arrowView.setLayoutParams(smallLayoutParams); // 设置箭头布局参数

        // 处理左侧按钮点击事件
        Button leftButton = this.view.findViewById(R.id.leftbtn); // 获取左侧按钮
        leftButton.setOnClickListener(v -> {
            // todo 添加复制功能
            //showFang.putCopyStringsToClipboard(); // 复制内容到剪贴板
            // TipsNetHelper.copyToClipboard(getActivity(), content);
            Toast.makeText(activity, "已复制到剪贴板", Toast.LENGTH_SHORT).show(); // 提示信息
        });

        // 处理右侧按钮点击事件
        Button rightButton = this.view.findViewById(R.id.rightbtn); // 获取右侧按钮
        rightButton.setOnClickListener(v -> {
            Intent intent = new Intent(activity, TipsFragmentActivity.class); // 创建Intent
            // todo 待完善
            //intent.putExtra("title", content); // 传递标题
            intent.putExtra("isFang", "false"); // 传递标识
            activity.startActivity(intent); // 启动新的Activity
        });

        // 配置Wrapper布局参数并添加视图
        LinearLayout wrapper = this.view.findViewById(R.id.wrapper); // 获取Wrapper布局
        wrapper.setLayoutParams(largeLayoutParams); // 设置布局参数
        this.mGroup.addView(this.view); // 将视图添加到根视图中

        return super.onCreateView(layoutInflater, viewGroup, bundle); // 返回父类的视图
    }

    /**
     * 设置适配器数据源
     * @param groups 数据源
     */
    public void setAdapterSource(Context context, ArrayList<GroupEntity> groups) {

        //检索所有的相关药方
        if (adapter == null) {
            adapter = new NoFooterAdapter(context, groups);
        }

    }

    private RecyclerView rvList;

    @Override
    public void onDestroyView() {
        this.mGroup.removeView(this.view);
        super.onDestroyView();
    }
}
