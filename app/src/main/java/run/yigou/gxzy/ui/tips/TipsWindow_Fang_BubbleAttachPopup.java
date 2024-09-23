/*
 * 项目名: AndroidProject
 * 类名: TipsWindow.java
 * 包名: run.yigou.gxzy.ui.tips.TipsWindow
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月06日 08:10:28
 * 上次修改时间: 2024年09月06日 08:10:28
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.donkingliang.groupedadapter.adapter.GroupedRecyclerViewAdapter;
import com.donkingliang.groupedadapter.holder.BaseViewHolder;
import com.lxj.xpopup.core.BubbleAttachPopupView;
import com.lxj.xpopup.util.XPopupUtils;

import java.util.ArrayList;

import run.yigou.gxzy.R;
import run.yigou.gxzy.ui.tips.tipsutils.DataBeans.Show_Fan_Yao_MingCi;
import run.yigou.gxzy.ui.tips.adapter.NoFooterAdapter;
import run.yigou.gxzy.ui.tips.entity.GroupModel;

@SuppressLint("ViewConstructor")
public class TipsWindow_Fang_BubbleAttachPopup extends BubbleAttachPopupView {
    private RecyclerView rvList;
    //private StickyHeaderLayout stickyLayout;
    private Context mContext;
    private String fanyao_name;
    // private static ShowFanYao fanYao;

    // private static NoFooterAdapter adapter;

    public TipsWindow_Fang_BubbleAttachPopup(@NonNull Context context, String fanyao) {
        super(context);
        mContext = context;
        fanyao_name = fanyao;
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.tips_windows_activity_sticky_list;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        setBubbleShadowSize(XPopupUtils.dp2px(getContext(), 2));
        //setBubbleShadowColor(Color.RED);
        setArrowWidth(XPopupUtils.dp2px(getContext(), 5f));
        setArrowHeight(XPopupUtils.dp2px(getContext(), 8f));
//                                .setBubbleRadius(100)
        setArrowRadius(XPopupUtils.dp2px(getContext(), 2f));
        rvList = findViewById(R.id.sticky_rv_list);
        //stickyLayout = (StickyHeaderLayout) findViewById(R.id.sticky_layout);

        rvList.setLayoutManager(new LinearLayoutManager(mContext));
        //加载药方和药物检查实例

        Show_Fan_Yao_MingCi fanYao = Show_Fan_Yao_MingCi.getInstance();
        ArrayList<Show_Fan_Yao_MingCi> mingCiList= Show_Fan_Yao_MingCi.getInstance().showFang(fanyao_name);

        NoFooterAdapter adapter = new NoFooterAdapter(mContext, GroupModel.getGroups(mingCiList,fanyao_name));

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
    }
//    // 设置最大宽度，看需要而定，
//    @Override
//    protected int getMaxWidth() {
//        Log.d(tag, "getMaxWidth: "+super.getMaxWidth());
//        return super.getMaxWidth();
//    }
//    String tag = "TipsWindow";
//    // 设置最大高度，看需要而定
//    @Override
//    protected int getMaxHeight() {
//        Log.d(tag, "getMaxHeight: "+super.getMaxHeight());
//        return super.getMaxHeight();
//    }
//    @Override
//    protected int getPopupHeight() {
//        return (int) (XPopupUtils.getScreenHeight(getContext())*.6f);
//    }
//@Override
//public boolean onTouchEvent(MotionEvent event) {
//
//    Log.d("TipsWindow", "onTouchEvent: X: "+event.getX()+" Y: "+event.getY());
//    return super.onTouchEvent(event);
//}
}
