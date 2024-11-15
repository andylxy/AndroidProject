package run.yigou.gxzy.ui.tips.widget;

import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.Nullable;

import run.yigou.gxzy.R;
import run.yigou.gxzy.ui.activity.TipsFragmentActivity;
import run.yigou.gxzy.ui.tips.tipsutils.TipsNetHelper;
import run.yigou.gxzy.ui.tips.tipsutils.TipsSingleData;

/* loaded from: classes.dex */
public class TipsLittleTextViewWindow extends TipsLittleWindow {

    private ViewGroup mGroup;
    private Rect rect;
    private View view;
    private String yao;
    private final TipsSingleData tipsSingleData;


    public TipsLittleTextViewWindow() {
        this.tipsSingleData = TipsSingleData.getInstance();

    }

    @Override
    public void show(FragmentManager fragmentManager) {
        super.show(fragmentManager);
        tipsSingleData.tipsLittleWindowStack.add(this);
    }

    @Override
    public void dismiss() {
        super.dismiss();
        tipsSingleData.tipsLittleWindowStack.remove(this);
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

    /**
     * 创建视图
     *
     * @param layoutInflater 布局填充器
     * @param viewGroup 父容器
     * @param bundle 保存实例状态的Bundle
     * @return 返回创建的视图
     */
    @Override
    @Nullable
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
            arrowDirection = TipsArrowView.UP;
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
            arrowDirection = TipsArrowView.DOWN;
        }

        // 设置关闭按钮的点击事件，点击时关闭当前视图
        ((Button) this.view.findViewById(R.id.maskbtnYao)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TipsLittleTextViewWindow.this.dismiss();
            }
        });

        // 设置文本内容
        final TextView textLabel = this.view.findViewById(R.id.textview);
        // 设置文本内容为"未找到"，如果yao有值则设置为yao的内容
        textLabel.setText("未找到");
        final String displayText = this.yao != null ? this.yao : "";
        textLabel.setText(TipsNetHelper.getSpanString(displayText));
        textLabel.setMovementMethod(LocalLinkMovementMethod.getInstance());

        // 设置箭头方向
        TipsArrowView arrowView = this.view.findViewById(R.id.arrow);
        arrowView.setDirection(arrowDirection);
        arrowView.setLayoutParams(arrowLayoutParams);

//        // 设置左侧按钮点击事件，点击时将文本内容复制到剪贴板
//        ((Button) this.view.findViewById(R.id.leftbtn)).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                TipsNetHelper.copyToClipboard(getActivity(), textLabel.getText().toString());
//                Toast.makeText(getActivity(), "已复制到剪贴板", Toast.LENGTH_SHORT).show();
//            }
//        });

//        // 设置右侧按钮点击事件，点击时启动MainActivity
//        ((Button) this.view.findViewById(R.id.rightbtn)).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent(TipsLittleTextViewWindow.this.getActivity(), TipsFragmentActivity.class);
//                intent.putExtra("title", displayText);
//                intent.putExtra("isFang", "false");
//                intent.putExtra("yaoZheng", "true");
//                TipsLittleTextViewWindow.this.getActivity().startActivity(intent);
//            }
//        });

        // 设置内容布局参数并添加到父布局中
        ((LinearLayout) this.view.findViewById(R.id.wrapper)).setLayoutParams(contentLayoutParams);
        this.mGroup.addView(this.view);
        // 调用父类的onCreateView方法
        return super.onCreateView(layoutInflater, viewGroup, bundle);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
       //  dismiss();
        this.mGroup.removeView(this.view);

    }
}
