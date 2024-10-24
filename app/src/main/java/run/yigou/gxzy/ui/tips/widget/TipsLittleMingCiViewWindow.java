package run.yigou.gxzy.ui.tips.widget;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
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

import java.util.ArrayList;

import run.yigou.gxzy.R;
import run.yigou.gxzy.ui.activity.TipsFragmentActivity;
import run.yigou.gxzy.ui.tips.adapter.NoFooterAdapter;
import run.yigou.gxzy.ui.tips.entity.GroupEntity;
import run.yigou.gxzy.ui.tips.tipsutils.TipsSingleData;

public class TipsLittleMingCiViewWindow extends TipsLittleWindow {

    private String fang;
    private ViewGroup mGroup;
    private Rect rect;
    private View view;
    private final TipsSingleData tips_single_data = TipsSingleData.getInstance();
    private NoFooterAdapter adapter;
    private RecyclerView rvList;


    @Override
    public void show(FragmentManager fragmentManager) {
        super.show(fragmentManager);
        tips_single_data.tipsLittleWindowStack.add(this);
    }

    @Override
    public void dismiss() {
        super.dismiss();
        tips_single_data.tipsLittleWindowStack.remove(this);
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

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        // 获取当前活动，确保不为null
        Activity activity = getActivity();
        if (activity == null) {
            throw new IllegalStateException("Activity cannot be null."); // 如果活动为null，则抛出异常
        }

        // 获取窗口的根视图和屏幕信息
        this.mGroup = (ViewGroup) activity.getWindow().getDecorView(); // 获取窗口的根视图
        int height = this.mGroup.getHeight(); // 获取视图高度
        int width = this.mGroup.getWidth(); // 获取视图宽度

        // 计算最小值，用于视图的大小
        int minSize = Math.min(50, (int) (width / 18)); // 计算最小尺寸

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
        this.view = inflateLayout(layoutInflater, centerY, midHeight, width, height, minSize, centerX, smallLayoutParams, largeLayoutParams);

        // 设置按钮点击事件
        setupButtonClicks(activity, this.view);

        // 配置RecyclerView
        setupRecyclerView(this.view, activity);

        // 配置Wrapper布局参数并添加视图
        configureWrapperAndAddView(this.view, largeLayoutParams, centerY, midHeight, smallLayoutParams);

        return super.onCreateView(layoutInflater, viewGroup, bundle); // 返回父类的视图
    }

    @SuppressLint("InflateParams")
    private View inflateLayout(LayoutInflater layoutInflater, int centerY, int midHeight, int width, int height, int minSize, int centerX, FrameLayout.LayoutParams smallLayoutParams, FrameLayout.LayoutParams largeLayoutParams) {
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
        return this.view;
    }

    private void setupButtonClicks(Activity activity, View view) {
        // 处理关闭按钮点击事件
        Button closeButton = view.findViewById(R.id.maskbtn); // 获取关闭按钮
        closeButton.setOnClickListener(v -> dismiss()); // 关闭视图

        // 处理左侧按钮点击事件
        Button leftButton = view.findViewById(R.id.leftbtn); // 获取左侧按钮
        leftButton.setOnClickListener(v -> {
            // todo 添加复制功能
            Toast.makeText(activity, "已复制到剪贴板", Toast.LENGTH_SHORT).show(); // 提示信息
        });

        // 处理右侧按钮点击事件
        Button rightButton = view.findViewById(R.id.rightbtn); // 获取右侧按钮
        rightButton.setOnClickListener(v -> {
            Intent intent = new Intent(activity, TipsFragmentActivity.class); // 创建Intent
            intent.putExtra("isFang", "false"); // 传递标识
            activity.startActivity(intent); // 启动新的Activity
        });
    }

    private void setupRecyclerView(View view, Activity activity) {
        this.rvList = view.findViewById(R.id.include_tips_windows_sticky_list).findViewById(R.id.sticky_rv_list);
        this.rvList.setLayoutManager(new LinearLayoutManager(activity));
        this.rvList.setAdapter(adapter);
    }

    private void configureWrapperAndAddView(View view, FrameLayout.LayoutParams largeLayoutParams, int centerY, int midHeight, FrameLayout.LayoutParams smallLayoutParams) {
        // 设置箭头方向
        TipsArrowView tipsArrowView = view.findViewById(R.id.arrow); // 获取箭头视图
        tipsArrowView.setDirection(centerY < midHeight ? TipsArrowView.UP : TipsArrowView.DOWN); // 设置箭头方向
        tipsArrowView.setLayoutParams(smallLayoutParams); // 设置箭头布局参数

        // 配置Wrapper布局参数并添加视图
        LinearLayout wrapper = view.findViewById(R.id.wrapper); // 获取Wrapper布局
        wrapper.setLayoutParams(largeLayoutParams); // 设置布局参数
        this.mGroup.addView(view); // 将视图添加到根视图中
    }

    /**
     * 设置适配器数据源
     * @param groups 数据源
     */
    public void setAdapterSource(Context context, ArrayList<GroupEntity> groups) {
        if (adapter == null) {
            adapter = new NoFooterAdapter(context, groups);
            adapter.setOnHeaderClickListener((adapter, holder, groupPosition) -> {
                // 处理组头点击事件
            });

            adapter.setOnChildClickListener((adapter, holder, groupPosition, childPosition) -> {
                // 处理子项点击事件
            });
        }
    }

    @Override
    public void onDestroyView() {
        this.mGroup.removeView(this.view);
        super.onDestroyView();
    }
}
