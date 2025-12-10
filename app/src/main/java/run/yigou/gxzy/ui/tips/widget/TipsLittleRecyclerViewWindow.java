package run.yigou.gxzy.ui.tips.widget;

import android.content.Context;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hjq.http.EasyLog;

import java.util.ArrayList;
import java.util.List;

import run.yigou.gxzy.R;
import run.yigou.gxzy.ui.tips.adapter.RefactoredPopupAdapter;
import run.yigou.gxzy.ui.tips.entity.GroupData;
import run.yigou.gxzy.ui.tips.entity.GroupEntity;
import run.yigou.gxzy.ui.tips.entity.ItemData;
import run.yigou.gxzy.ui.tips.utils.PopupDataAdapter;

/**
 * RecyclerView弹窗中间基类
 * 为带RecyclerView的弹窗提供通用功能
 */
public abstract class TipsLittleRecyclerViewWindow extends TipsLittleWindow {

    protected RecyclerView recyclerView;
    protected RefactoredPopupAdapter adapter;
    protected List<GroupData> groupDataList;
    protected List<List<ItemData>> itemDataList;

    @Override
    protected View createContentView(LayoutInflater inflater, ViewGroup container) {
        // 查找RecyclerView
        recyclerView = this.view.findViewById(R.id.include_tips_windows_sticky_list)
                .findViewById(R.id.sticky_rv_list);

        // 设置LayoutManager
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        return recyclerView;
    }

    @Override
    protected void bindData() {
        EasyLog.print("=== TipsLittleRecyclerViewWindow.bindData() ===");

        if (adapter == null) {
            EasyLog.print("❌ adapter为null! setAdapterSource未被调用");
            return;
        }

        EasyLog.print("✅ adapter不为null, itemCount: " + adapter.getItemCount());
        recyclerView.setAdapter(adapter);
        EasyLog.print("RecyclerView.setAdapter() 完成");
    }

    /**
     * 设置适配器数据源
     * 将旧的GroupEntity数据转换为新架构的GroupData+ItemData
     *
     * @param context 上下文
     * @param groups  旧架构的数据源
     */
    public void setAdapterSource(Context context, ArrayList<GroupEntity> groups) {
        EasyLog.print("=== TipsLittleRecyclerViewWindow.setAdapterSource() ===");
        EasyLog.print("Context: " + context);
        EasyLog.print("groups size: " + (groups != null ? groups.size() : "null"));

        if (groups == null || groups.isEmpty()) {
            EasyLog.print("❌ groups为null或空");
            return;
        }

        // 使用PopupDataAdapter转换数据
        Pair<List<GroupData>, List<List<ItemData>>> convertedData = 
                PopupDataAdapter.convert(groups);

        this.groupDataList = convertedData.first;
        this.itemDataList = convertedData.second;

        EasyLog.print("数据转换完成:");
        EasyLog.print("  groupDataList size: " + groupDataList.size());
        EasyLog.print("  itemDataList size: " + itemDataList.size());

        // 创建新的RefactoredPopupAdapter
        if (adapter == null) {
            EasyLog.print("创建RefactoredPopupAdapter");
            adapter = new RefactoredPopupAdapter(context, groupDataList, itemDataList);
            EasyLog.print("RefactoredPopupAdapter创建完成, itemCount: " + adapter.getItemCount());
        } else {
            EasyLog.print("adapter已存在, 更新数据");
            adapter.updateData(groupDataList, itemDataList);
        }

        EasyLog.print("setAdapterSource() 完成");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (recyclerView != null) {
            recyclerView.setAdapter(null);
            recyclerView = null;
        }
        adapter = null;
        groupDataList = null;
        itemDataList = null;
    }
}
